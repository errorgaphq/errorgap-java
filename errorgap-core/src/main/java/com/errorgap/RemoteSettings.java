package com.errorgap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The project's remote settings: what errorgap wants this app to send.
 * Someone can turn errors or performance data off for a project in errorgap
 * (Project settings › What SDKs send) and running apps follow within a poll
 * interval, with no deploy.
 *
 * <p>Read from {@code GET /api/projects/:slug/config} on a daemon thread.
 * Until the first answer, and whenever errorgap cannot be reached, everything
 * stays on.
 */
public final class RemoteSettings {
    public enum Kind { ERRORS, APM, LOGS }

    private static final long DEFAULT_INTERVAL_SECONDS = 600;
    private static final long MIN_INTERVAL_SECONDS = 60;
    private static final Pattern INTERVAL = Pattern.compile("\"poll_interval_seconds\"\\s*:\\s*(\\d+)");

    private final Supplier<Configuration> configuration;
    private final HttpClient http;
    private volatile boolean errorsOff;
    private volatile boolean apmOff;
    private volatile boolean logsOff;
    private volatile long intervalSeconds = DEFAULT_INTERVAL_SECONDS;
    private volatile Thread poller;
    private volatile boolean stopped;

    RemoteSettings(Supplier<Configuration> configuration, HttpClient http) {
        this.configuration = configuration;
        this.http = http;
    }

    /** False only when errorgap has said to stop sending this kind of data. */
    public boolean enabled(Kind kind) {
        if (!configuration.get().isRemoteSettings()) {
            return true;
        }
        ensurePolling();
        return switch (kind) {
            case ERRORS -> !errorsOff;
            case APM -> !apmOff;
            case LOGS -> !logsOff;
        };
    }

    /**
     * Fetch now, on this thread. Returns whether the settings could be read.
     * One read at a time, so an older answer can never overwrite a newer one.
     */
    public synchronized boolean refresh() {
        Configuration cfg = configuration.get();
        try {
            String base = cfg.getEndpoint().endsWith("/")
                ? cfg.getEndpoint().substring(0, cfg.getEndpoint().length() - 1)
                : cfg.getEndpoint();
            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/projects/" + cfg.getProjectSlug() + "/config"))
                .timeout(cfg.getTimeout())
                .header("user-agent", "errorgap-java/" + Version.VERSION)
                .GET();
            if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
                request.header("x-errorgap-project-key", cfg.getApiKey());
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() != 200 || body == null || !body.trim().startsWith("{")) {
                return false;
            }
            // An absent field means on.
            errorsOff = isFalse(body, "errors_enabled");
            apmOff = isFalse(body, "apm_enabled");
            logsOff = isFalse(body, "logs_enabled");
            Matcher interval = INTERVAL.matcher(body);
            long seconds = interval.find() ? Long.parseLong(interval.group(1)) : 0;
            intervalSeconds = seconds >= MIN_INTERVAL_SECONDS ? seconds : DEFAULT_INTERVAL_SECONDS;
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception unreadable) {
            return false;
        }
    }

    void stop() {
        stopped = true;
        Thread thread = poller;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private static boolean isFalse(String body, String field) {
        return Pattern.compile("\"" + field + "\"\\s*:\\s*false").matcher(body).find();
    }

    private void ensurePolling() {
        synchronized (pollerLock) {
            startPoller();
        }
    }

    private final Object pollerLock = new Object();

    private void startPoller() {
        if (poller != null || stopped) {
            return;
        }
        Thread thread = new Thread(() -> {
            while (!stopped) {
                refresh();
                try {
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }, "errorgap-settings");
        thread.setDaemon(true);
        poller = thread;
        thread.start();
    }
}
