package com.errorgap.spring;

import com.errorgap.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return cfg;
    }

    @Bean(destroyMethod = "")
    @ConditionalOnMissingBean
    public Client errorgapClient(com.errorgap.Configuration cfg) {
        return new Client(cfg);
    }

    @Bean
    @ConditionalOnMissingBean
    public ErrorgapWebExceptionHandler errorgapWebExceptionHandler(Client client) {
        return new ErrorgapWebExceptionHandler(client);
    }
}
