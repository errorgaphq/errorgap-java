package com.errorgap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemoteSettingsTest {

    @Test
    void aSwitchedOffKindIsNotSentAndEverythingStaysOnWhenUnreadable() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        ing.settings = "{\"errors_enabled\":false,\"apm_enabled\":true,\"poll_interval_seconds\":600}";
        Configuration cfg = new Configuration()
            .setEndpoint(ing.endpoint()).setProjectSlug("demo").setApiKey("key-demo")
            .setAsync(false).setApmEnabled(true).setRemoteSettings(true);
        try (Client client = new Client(cfg)) {
            assertTrue(client.remoteSettings().refresh());
            assertFalse(client.remoteSettings().enabled(RemoteSettings.Kind.ERRORS));
            assertTrue(client.remoteSettings().enabled(RemoteSettings.Kind.APM));

            assertEquals("errors are switched off for this project", client.notify(new RuntimeException("x")).body);
            assertEquals(0, ing.requests().size());

            // The switch is turned back on.
            ing.settings = "{\"errors_enabled\":true}";
            assertTrue(client.remoteSettings().refresh());
            assertTrue(client.notify(new RuntimeException("y")).success());
            assertEquals(1, ing.requests().size());

            // Unreadable settings keep what was known.
            ing.settings = null;
            assertFalse(client.remoteSettings().refresh());
            assertTrue(client.remoteSettings().enabled(RemoteSettings.Kind.ERRORS));
        } finally {
            ing.close();
        }

        try (Client off = new Client(new Configuration().setEndpoint("http://127.0.0.1:1")
            .setProjectSlug("demo").setRemoteSettings(false))) {
            assertTrue(off.remoteSettings().enabled(RemoteSettings.Kind.ERRORS));
        }
    }
}
