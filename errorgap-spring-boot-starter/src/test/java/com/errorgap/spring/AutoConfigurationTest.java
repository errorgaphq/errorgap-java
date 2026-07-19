package com.errorgap.spring;

import com.errorgap.Client;
import com.errorgap.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ErrorgapAutoConfiguration.class));

    @Test
    void doesNotRegisterBeansWithoutProjectSlug() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(Client.class);
            assertThat(ctx).doesNotHaveBean(Configuration.class);
        });
    }

    @Test
    void registersBeansWhenProjectSlugSet() {
        runner.withPropertyValues(
            "errorgap.endpoint=https://errorgap.example.com",
            "errorgap.project-slug=demo",
            "errorgap.api-key=flk_test"
        ).run(ctx -> {
            assertThat(ctx).hasSingleBean(Configuration.class);
            assertThat(ctx).hasSingleBean(Client.class);
            assertThat(ctx).hasSingleBean(ErrorgapWebExceptionHandler.class);

            Configuration cfg = ctx.getBean(Configuration.class);
            assertThat(cfg.getProjectSlug()).isEqualTo("demo");
            assertThat(cfg.getApiKey()).isEqualTo("flk_test");
            assertThat(cfg.getEndpoint()).isEqualTo("https://errorgap.example.com");
        });
    }

    @Test
    void registersApmBeansAndConfiguration() {
        runner.withPropertyValues(
            "errorgap.project-slug=demo",
            "errorgap.apm-enabled=true",
            "errorgap.apm-sample-rate=0.75"
        ).run(ctx -> {
            assertThat(ctx).hasSingleBean(QuerySpanCollector.class);
            assertThat(ctx).hasSingleBean(ErrorgapWebFilter.class);
            assertThat(ctx).hasSingleBean(ErrorgapApm.class);
            assertThat(ctx).hasSingleBean(ErrorgapDataSourceBeanPostProcessor.class);
            assertThat(ctx.getBean(Configuration.class).isApmEnabled()).isTrue();
            assertThat(ctx.getBean(Configuration.class).getApmSampleRate()).isEqualTo(0.75);
        });
    }
}
