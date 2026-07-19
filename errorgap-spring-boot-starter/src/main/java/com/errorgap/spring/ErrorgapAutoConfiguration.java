package com.errorgap.spring;

import com.errorgap.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ErrorgapProperties.class)
@ConditionalOnProperty(prefix = "errorgap", name = "project-slug")
public class ErrorgapAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public com.errorgap.Configuration errorgapConfiguration(ErrorgapProperties props) {
        com.errorgap.Configuration cfg = new com.errorgap.Configuration();
        if (props.getEndpoint() != null) cfg.setEndpoint(props.getEndpoint());
        if (props.getProjectSlug() != null) cfg.setProjectSlug(props.getProjectSlug());
        if (props.getProjectId() != null) cfg.setProjectId(props.getProjectId());
        if (props.getApiKey() != null) cfg.setApiKey(props.getApiKey());
        if (props.getEnvironment() != null) cfg.setEnvironment(props.getEnvironment());
        if (props.getRelease() != null) cfg.setRelease(props.getRelease());
        if (props.getAsync() != null) cfg.setAsync(props.getAsync());
        if (props.getTimeoutSeconds() != null) cfg.setTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        if (props.getApmEnabled() != null) cfg.setApmEnabled(props.getApmEnabled());
        if (props.getApmSampleRate() != null) cfg.setApmSampleRate(props.getApmSampleRate());
        if (props.getRootDirectory() != null) cfg.setRootDirectory(props.getRootDirectory());
        return cfg;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public Client errorgapClient(com.errorgap.Configuration cfg) {
        return new Client(cfg);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorgapWebExceptionHandler errorgapWebExceptionHandler(Client client) {
        return new ErrorgapWebExceptionHandler(client);
    }

    @Bean
    @ConditionalOnProperty(prefix = "errorgap", name = "apm-enabled", havingValue = "true")
    public QuerySpanCollector errorgapQuerySpanCollector() {
        return new QuerySpanCollector();
    }

    @Bean
    @ConditionalOnProperty(prefix = "errorgap", name = "apm-enabled", havingValue = "true")
    public ErrorgapWebFilter errorgapWebFilter(Client client, QuerySpanCollector spans) {
        return new ErrorgapWebFilter(client, spans);
    }

    @Bean
    @ConditionalOnProperty(prefix = "errorgap", name = "apm-enabled", havingValue = "true")
    public ErrorgapApm errorgapApm(Client client, QuerySpanCollector spans) {
        return new ErrorgapApm(client, spans);
    }

    @Bean
    @ConditionalOnProperty(prefix = "errorgap", name = "apm-enabled", havingValue = "true")
    public static ErrorgapDataSourceBeanPostProcessor errorgapDataSourceBeanPostProcessor(
        ObjectProvider<QuerySpanCollector> spans
    ) {
        return new ErrorgapDataSourceBeanPostProcessor(spans::getObject);
    }
}
