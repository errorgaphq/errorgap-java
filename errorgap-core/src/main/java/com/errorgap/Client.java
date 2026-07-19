package com.errorgap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements AutoCloseable {

    private volatile Configuration configuration;
    private final HttpClient httpClient;
    private final LinkedBlockingQueue<Delivery> queue;
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final Thread worker;
    private volatile boolean running = true;

    public Client(Configuration configuration) {
        this.configuration = configuration;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(configuration.getTimeout())
            .build();
        this.queue = new LinkedBlockingQueue<>(configuration.getQueueSize());
        this.worker = new Thread(this::loop, "errorgap-delivery");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public Configuration configuration() {
        return configuration;
    }

    public void configure(Configuration configuration) {
        this.configuration = configuration;
    }

    public Result notify(Throwable throwable) {
        return notify(throwable, new NoticeOptions(), false);
    }

    public Result notify(Throwable throwable, NoticeOptions options) {
        return notify(throwable, options, false);
    }

    public Result notify(Throwable throwable, NoticeOptions options, boolean sync) {
        try {
            configuration.validate();
            Notice notice = Notice.fromThrowable(throwable, configuration, options);
            return submit(new Delivery(noticesUrl(), Json.encode(notice.toMap())), sync);
        } catch (Throwable caught) {
            log(caught.getClass().getSimpleName() + ": " + caught.getMessage());
            return new Result(null, null, caught, false);
        }
    }

    public Result notifyTransaction(ApmTransaction transaction) {
        return notifyTransaction(transaction, false);
    }

    public Result notifyTransaction(ApmTransaction transaction, boolean sync) {
        try {
            configuration.validate();
            if (!configuration.isApmEnabled()
                || configuration.getApmSampleRate() <= 0
                || (configuration.getApmSampleRate() < 1
                    && ThreadLocalRandom.current().nextDouble() >= configuration.getApmSampleRate())) {
                return new Result(204, null, null, false);
            }
            return submit(
                new Delivery(transactionsUrl(), Json.encode(transaction.toMap(configuration))),
                sync
            );
        } catch (Throwable caught) {
            log(caught.getClass().getSimpleName() + ": " + caught.getMessage());
            return new Result(null, null, caught, false);
        }
    }

    private Result submit(Delivery delivery, boolean sync) {
        if (sync || !configuration.isAsync()) {
            return deliver(delivery);
        }
        // Count at enqueue time: if the worker decremented only around
        // delivery, flush() could observe an empty queue in the gap
        // between poll() and the in-flight increment and return early.
        inFlight.incrementAndGet();
        if (!queue.offer(delivery)) {
            inFlight.decrementAndGet();
            log("payload dropped, queue full");
            return new Result(null, null, new RuntimeException("queue full"), false);
        }
        return new Result(202, null, null, true);
    }

    public void flush(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (inFlight.get() > 0) {
            if (System.nanoTime() >= deadline) {
                return;
            }
            //noinspection BusyWait
            Thread.sleep(10);
        }
    }

    public void shutdown(Duration timeout) throws InterruptedException {
        flush(timeout);
        running = false;
        worker.interrupt();
        worker.join(timeout.toMillis());
    }

    @Override
    public void close() {
        try {
            shutdown(Duration.ofSeconds(5));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void loop() {
        while (running) {
            try {
                Delivery delivery = queue.poll(100, TimeUnit.MILLISECONDS);
                if (delivery != null) {
                    try {
                        deliver(delivery);
                    } finally {
                        inFlight.decrementAndGet();
                    }
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    Result deliver(Delivery delivery) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(delivery.url()))
            .timeout(configuration.getTimeout())
            .header("content-type", "application/json")
            .header("user-agent", "errorgap-java/" + Version.VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(delivery.body()));

        if (configuration.getApiKey() != null && !configuration.getApiKey().isBlank()) {
            reqBuilder.header("x-errorgap-project-key", configuration.getApiKey());
        }

        try {
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log("delivery failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return new Result(response.statusCode(), response.body(), null, false);
        } catch (Throwable caught) {
            log(caught.getClass().getSimpleName() + ": " + caught.getMessage());
            return new Result(null, null, caught, false);
        }
    }

    private String noticesUrl() {
        return projectUrl("notices");
    }

    private String transactionsUrl() {
        return projectUrl("transactions");
    }

    private String projectUrl(String resource) {
        String base = configuration.getEndpoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/projects/" + configuration.getProjectSlug() + "/" + resource;
    }

    private void log(String message) {
        if (configuration.getLogger() != null) {
            configuration.getLogger().accept("[errorgap] " + message);
        }
    }

    record Delivery(String url, String body) {
    }

    public static final class Result {
        public final Integer status;
        public final String body;
        public final Throwable error;
        public final boolean queued;

        public Result(Integer status, String body, Throwable error, boolean queued) {
            this.status = status;
            this.body = body;
            this.error = error;
            this.queued = queued;
        }

        public boolean success() {
            return error == null && status != null && status >= 200 && status < 300;
        }
    }
}
