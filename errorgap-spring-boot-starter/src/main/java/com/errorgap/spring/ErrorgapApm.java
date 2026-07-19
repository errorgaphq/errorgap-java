package com.errorgap.spring;

import com.errorgap.ApmTransaction;
import com.errorgap.Client;
import com.errorgap.NoticeOptions;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ErrorgapApm {
    private final Client client;
    private final QuerySpanCollector spans;

    public ErrorgapApm(Client client, QuerySpanCollector spans) {
        this.client = client;
        this.spans = spans;
    }

    public void trackJob(String jobClass, String queue, Runnable operation) {
        long started = System.nanoTime();
        Throwable failure = null;
        spans.begin();
        try {
            operation.run();
        } catch (RuntimeException | Error caught) {
            failure = caught;
            throw caught;
        } finally {
            List<com.errorgap.ApmSpan> querySpans = spans.finish();
            if (failure != null) {
                Map<String, Object> context = new LinkedHashMap<>();
                context.put("source", "spring.ErrorgapApm");
                context.put("component", "spring.job");
                context.put("action", jobClass);
                client.notify(failure, new NoticeOptions()
                    .context(context)
                    .environment(Map.of("queue", queue == null ? "default" : queue)), true);
            }
            client.notifyTransaction(new ApmTransaction()
                .setKind("job")
                .setJobClass(jobClass)
                .setQueue(queue == null ? "default" : queue)
                .setStatusCode(failure == null ? 200 : 500)
                .setDurationMs((System.nanoTime() - started) / 1_000_000.0)
                .setSpans(querySpans));
        }
    }
}
