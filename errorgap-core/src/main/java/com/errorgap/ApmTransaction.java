package com.errorgap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ApmTransaction {
    private String kind = "web";
    private String method;
    private String path;
    private String pathRaw;
    private Integer statusCode;
    private double durationMs;
    private String environment;
    private Instant occurredAt = Instant.now();
    private List<ApmSpan> spans = new ArrayList<>();
    private String jobClass;
    private String queue;

    public String getKind() { return kind; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getPathRaw() { return pathRaw; }
    public Integer getStatusCode() { return statusCode; }
    public double getDurationMs() { return durationMs; }
    public List<ApmSpan> getSpans() { return List.copyOf(spans); }
    public String getJobClass() { return jobClass; }
    public String getQueue() { return queue; }
    public ApmTransaction setKind(String value) { this.kind = value; return this; }
    public ApmTransaction setMethod(String value) { this.method = value; return this; }
    public ApmTransaction setPath(String value) { this.path = value; return this; }
    public ApmTransaction setPathRaw(String value) { this.pathRaw = value; return this; }
    public ApmTransaction setStatusCode(Integer value) { this.statusCode = value; return this; }
    public ApmTransaction setDurationMs(double value) { this.durationMs = value; return this; }
    public ApmTransaction setEnvironment(String value) { this.environment = value; return this; }
    public ApmTransaction setOccurredAt(Instant value) { this.occurredAt = value; return this; }
    public ApmTransaction setSpans(List<ApmSpan> value) {
        this.spans = value == null ? new ArrayList<>() : new ArrayList<>(value);
        return this;
    }
    public ApmTransaction addSpan(ApmSpan value) { this.spans.add(value); return this; }
    public ApmTransaction setJobClass(String value) { this.jobClass = value; return this; }
    public ApmTransaction setQueue(String value) { this.queue = value; return this; }

    Map<String, Object> toMap(Configuration configuration) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", kind);
        if (method != null) map.put("method", method);
        if (path != null) map.put("path", path);
        if (pathRaw != null) map.put("path_raw", pathRaw);
        if (statusCode != null) map.put("status_code", statusCode);
        map.put("duration_ms", durationMs);
        map.put("environment", environment == null ? configuration.getEnvironment() : environment);
        map.put("occurred_at", (occurredAt == null ? Instant.now() : occurredAt).toString());
        map.put("spans", spans.stream().map(ApmSpan::toMap).toList());
        if (jobClass != null) map.put("job_class", jobClass);
        if (queue != null) map.put("queue", queue);
        return map;
    }
}
