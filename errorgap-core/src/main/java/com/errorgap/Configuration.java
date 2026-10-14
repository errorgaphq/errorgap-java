package com.errorgap;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public final class Configuration {
    public static final List<String> DEFAULT_FILTER_KEYS = List.of(
        "password", "password_confirmation", "token", "secret",
        "api_key", "authorization", "cookie"
    );

    private String endpoint;
    private String projectSlug;
    private String projectId;
    private String apiKey;
    private String environment;
    private String release;
    private String rootDirectory;
    private boolean async = true;
    private Consumer<String> logger;
    private List<String> filterKeys = DEFAULT_FILTER_KEYS;
    private Duration timeout = Duration.ofSeconds(5);
    private int queueSize = 100;
    private boolean apmEnabled;
    private double apmSampleRate = 1.0;
    private List<String> ignoreEnvironments = envList("ERRORGAP_IGNORE_ENVIRONMENTS");
    private java.util.function.UnaryOperator<java.util.Map<String, Object>> beforeNotify;
    private boolean remoteSettings = !"false".equalsIgnoreCase(
        String.valueOf(System.getenv("ERRORGAP_REMOTE_SETTINGS")).trim());

    public Configuration() {
        this.endpoint = envOr("ERRORGAP_ENDPOINT", "http://127.0.0.1:3030");
        this.projectSlug = System.getenv("ERRORGAP_PROJECT_SLUG");
        this.projectId = System.getenv("ERRORGAP_PROJECT_ID");
        this.apiKey = System.getenv("ERRORGAP_API_KEY");
        this.environment = envOr("ERRORGAP_ENVIRONMENT", "production");
        this.rootDirectory = System.getProperty("user.dir");
        this.apmEnabled = Boolean.parseBoolean(envOr("ERRORGAP_APM_ENABLED", "false"));
        this.apmSampleRate = parseSampleRate(envOr("ERRORGAP_APM_SAMPLE_RATE", "1"));
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static double parseSampleRate(String value) {
        try {
            return Math.max(0.0, Math.min(1.0, Double.parseDouble(value)));
        } catch (NumberFormatException ignored) {
            return 1.0;
        }
    }

    public String getEndpoint() { return endpoint; }
    public Configuration setEndpoint(String v) { this.endpoint = v; return this; }

    public String getProjectSlug() { return projectSlug; }
    public Configuration setProjectSlug(String v) { this.projectSlug = v; return this; }

    public String getProjectId() { return projectId; }
    public Configuration setProjectId(String v) { this.projectId = v; return this; }

    public String getApiKey() { return apiKey; }
    public Configuration setApiKey(String v) { this.apiKey = v; return this; }

    public String getEnvironment() { return environment; }
    public Configuration setEnvironment(String v) { this.environment = v; return this; }

    public String getRelease() { return release; }
    public Configuration setRelease(String v) { this.release = v; return this; }

    public String getRootDirectory() { return rootDirectory; }
    public Configuration setRootDirectory(String v) { this.rootDirectory = v; return this; }

    public boolean isAsync() { return async; }
    public Configuration setAsync(boolean v) { this.async = v; return this; }

    public Consumer<String> getLogger() { return logger; }
    public Configuration setLogger(Consumer<String> v) { this.logger = v; return this; }

    public List<String> getFilterKeys() { return filterKeys; }
    public Configuration setFilterKeys(List<String> v) { this.filterKeys = v; return this; }

    public Duration getTimeout() { return timeout; }
    public Configuration setTimeout(Duration v) { this.timeout = v; return this; }

    public int getQueueSize() { return queueSize; }
    public Configuration setQueueSize(int v) { this.queueSize = v; return this; }

    public boolean isApmEnabled() { return apmEnabled; }
    public Configuration setApmEnabled(boolean v) { this.apmEnabled = v; return this; }

    public double getApmSampleRate() { return apmSampleRate; }
    public Configuration setApmSampleRate(double v) {
        this.apmSampleRate = Math.max(0.0, Math.min(1.0, v));
        return this;
    }

    /**
     * Follow the project's settings in errorgap (Project settings › What SDKs
     * send): stop sending what is switched off there. On by default;
     * {@code ERRORGAP_REMOTE_SETTINGS=false} turns it off.
     */
    public boolean isRemoteSettings() { return remoteSettings; }
    public Configuration setRemoteSettings(boolean v) { this.remoteSettings = v; return this; }

    /** Environments in which nothing is sent. */
    public List<String> getIgnoreEnvironments() { return ignoreEnvironments; }
    public Configuration setIgnoreEnvironments(List<String> v) {
        this.ignoreEnvironments = v == null ? List.of() : v;
        return this;
    }

    public boolean isIgnoredEnvironment() {
        return environment != null && ignoreEnvironments.contains(environment);
    }

    public java.util.function.UnaryOperator<java.util.Map<String, Object>> getBeforeNotify() { return beforeNotify; }

    /**
     * Sees each notice (as a map) before it is sent and returns the notice to
     * send, changed or not, or {@code null} to drop it.
     */
    public Configuration setBeforeNotify(java.util.function.UnaryOperator<java.util.Map<String, Object>> v) {
        this.beforeNotify = v;
        return this;
    }

    private static List<String> envList(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .toList();
    }

    public void validate() {
        if (projectSlug == null || projectSlug.isBlank()) {
            throw new IllegalStateException("Errorgap projectSlug is required");
        }
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Errorgap endpoint is required");
        }
    }
}
