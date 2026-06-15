package io.errorgap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void defaultsWhenNothingProvided() {
        Configuration cfg = new Configuration();
        assertNotNull(cfg.getEndpoint());
        assertTrue(cfg.isAsync());
        assertTrue(cfg.getFilterKeys().contains("password"));
    }

    @Test
    void validateThrowsWhenProjectSlugMissing() {
        Configuration cfg = new Configuration();
        IllegalStateException ex = assertThrows(IllegalStateException.class, cfg::validate);
        assertTrue(ex.getMessage().contains("projectSlug"));
    }

    @Test
    void validatePassesWhenProjectSlugPresent() {
        Configuration cfg = new Configuration().setProjectSlug("demo");
        assertDoesNotThrow(cfg::validate);
    }
}
