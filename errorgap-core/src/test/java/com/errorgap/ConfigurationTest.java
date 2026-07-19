package com.errorgap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void defaultsWhenNothingProvided() {
        Configuration cfg = new Configuration();
        assertNotNull(cfg.getEndpoint());
        assertTrue(cfg.isAsync());
        assertTrue(cfg.getFilterKeys().contains("password"));
        assertTrue(cfg.getApmSampleRate() >= 0 && cfg.getApmSampleRate() <= 1);
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

    @Test
    void clampsApmSampleRate() {
        Configuration cfg = new Configuration().setApmEnabled(true).setApmSampleRate(2.5);
        assertTrue(cfg.isApmEnabled());
        assertEquals(1.0, cfg.getApmSampleRate());
        cfg.setApmSampleRate(-1);
        assertEquals(0.0, cfg.getApmSampleRate());
    }
}
