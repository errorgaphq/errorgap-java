package com.errorgap;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Notice {

    public final String projectId;
    public final String receivedAt;
    public final List<Map<String, Object>> errors;
    public final Map<String, Object> context;
    public final Map<String, Object> environment;
    public final Map<String, Object> session;
    public final Map<String, Object> params;

    private Notice(String projectId,
                   String receivedAt,
                   List<Map<String, Object>> errors,
                   Map<String, Object> context,
                   Map<String, Object> environment,
                   Map<String, Object> session,
                   Map<String, Object> params) {
        this.projectId = projectId;
        this.receivedAt = receivedAt;
        this.errors = errors;
        this.context = context;
        this.environment = environment;
        this.session = session;
        this.params = params;
    }

    public static Notice fromThrowable(Throwable throwable, Configuration config, NoticeOptions options) {
        if (options == null) {
            options = new NoticeOptions();
        }
        Map<String, Object> defaultContext = new LinkedHashMap<>();
        defaultContext.put("notifier", "errorgap-java");
        defaultContext.put("notifier_version", Version.VERSION);
        defaultContext.put("environment", config.getEnvironment());
        if (config.getRelease() != null) {
            defaultContext.put("release", config.getRelease());
        }
        if (config.getRootDirectory() != null) {
            defaultContext.put("root_directory", config.getRootDirectory());
        }
        if (options.context != null) {
            defaultContext.putAll(options.context);
        }

        Map<String, Object> err = new LinkedHashMap<>();
        err.put("type", throwable.getClass().getSimpleName().isEmpty()
            ? throwable.getClass().getName()
            : throwable.getClass().getSimpleName());
        err.put("message", throwable.getMessage() == null ? "" : throwable.getMessage());
        err.put("backtrace", Backtrace.fromThrowable(throwable, config.getRootDirectory())
            .stream()
            .map(Notice::frameToMap)
            .toList());

        return new Notice(
            config.getProjectId(),
            Instant.now().toString(),
            List.of(err),
            defaultContext,
            options.environment == null ? new LinkedHashMap<>() : options.environment,
            options.session == null ? new LinkedHashMap<>() : options.session,
            Filter.params(options.params, config.getFilterKeys())
        );
    }

    private static Map<String, Object> frameToMap(Backtrace.Frame frame) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (frame.file() != null) map.put("file", frame.file());
        if (frame.line() != null) map.put("line", frame.line());
        if (frame.function() != null) map.put("function", frame.function());
        map.put("in_app", frame.inApp());
        map.put("index", frame.index());
        return map;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (projectId != null) map.put("project_id", projectId);
        map.put("received_at", receivedAt);
        map.put("errors", errors);
        map.put("context", context);
        map.put("environment", environment);
        map.put("session", session);
        map.put("params", params);
        return map;
    }
}
