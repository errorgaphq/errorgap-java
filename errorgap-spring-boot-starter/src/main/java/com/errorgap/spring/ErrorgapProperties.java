package com.errorgap.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "errorgap")
public class ErrorgapProperties {

    private String endpoint;
    private String projectSlug;
    private String projectId;
    private String apiKey;
    private String environment;
    private String release;
    private Boolean async;
    private Integer timeoutSeconds;
    private Boolean apmEnabled;
    private Double apmSampleRate;
    private String rootDirectory;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getProjectSlug() { return projectSlug; }
    public void setProjectSlug(String projectSlug) { this.projectSlug = projectSlug; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getRelease() { return release; }
    public void setRelease(String release) { this.release = release; }

    public Boolean getAsync() { return async; }
    public void setAsync(Boolean async) { this.async = async; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Boolean getApmEnabled() { return apmEnabled; }
    public void setApmEnabled(Boolean apmEnabled) { this.apmEnabled = apmEnabled; }

    public Double getApmSampleRate() { return apmSampleRate; }
    public void setApmSampleRate(Double apmSampleRate) { this.apmSampleRate = apmSampleRate; }

    public String getRootDirectory() { return rootDirectory; }
    public void setRootDirectory(String rootDirectory) { this.rootDirectory = rootDirectory; }
}
