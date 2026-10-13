package com.errorgap;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HardeningTest {

    private static Configuration config(String endpoint) {
        return new Configuration().setEndpoint(endpoint).setProjectSlug("demo").setAsync(false);
    }

    @Test
    void a429PausesSendingAndOnlyNetworkAndServerErrorsAreRetried() throws Exception {
        FakeIngestor ing = new FakeIngestor(429, "{}");
        try (Client client = new Client(config(ing.endpoint()))) {
            assertEquals(429, client.notify(new RuntimeException("one")).status);
            assertTrue(client.paused());
            assertInstanceOf(Client.RateLimitedException.class, client.notify(new RuntimeException("two")).error);
            assertEquals(1, ing.requests().size());

            assertTrue(Client.retryable(new Client.Result(500, null, null, false)));
            assertTrue(Client.retryable(new Client.Result(null, null, new IOException("refused"), false)));
            assertFalse(Client.retryable(new Client.Result(422, null, null, false)));
            assertFalse(Client.retryable(new Client.Result(429, null, null, false)));
            assertFalse(Client.retryable(new Client.Result(null, null, new Client.RateLimitedException(), false)));
        } finally {
            ing.close();
        }
    }

    @Test
    void beforeNotifyCanDropOrChangeANoticeAndAThrowingHookIsSkipped() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try (
            Client drop = new Client(config(ing.endpoint()).setBeforeNotify(notice -> null));
            Client change = new Client(config(ing.endpoint()).setBeforeNotify(notice -> {
                Map<String, Object> changed = new LinkedHashMap<>(notice);
                changed.put("params", Map.of("team", "billing"));
                return changed;
            }));
            Client broken = new Client(config(ing.endpoint()).setBeforeNotify(notice -> {
                throw new IllegalStateException("bug in the hook");
            }))
        ) {
            assertEquals("dropped by beforeNotify", drop.notify(new RuntimeException("no")).body);
            assertTrue(change.notify(new RuntimeException("yes")).success());
            assertTrue(broken.notify(new RuntimeException("still sent")).success());

            List<FakeIngestor.CapturedRequest> requests = ing.requests();
            assertEquals(2, requests.size());
            assertTrue(requests.get(0).body.contains("\"team\":\"billing\""));
            assertTrue(requests.get(1).body.contains("still sent"));
        } finally {
            ing.close();
        }
    }

    @Test
    void nothingIsSentInAnIgnoredEnvironment() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try (Client client = new Client(
            config(ing.endpoint()).setEnvironment("test").setIgnoreEnvironments(List.of("test")))) {
            assertEquals("ignored environment", client.notify(new RuntimeException("x")).body);
            assertEquals(0, ing.requests().size());
        } finally {
            ing.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void sensitiveKeysAreMaskedInsideListsAndInTheSession() {
        Map<String, Object> filtered = Filter.params(
            Map.of(
                "users", List.of(Map.of("password", "p", "name", "Ada")),
                "long", "x".repeat(10_000)),
            Configuration.DEFAULT_FILTER_KEYS);
        Map<String, Object> user = ((List<Map<String, Object>>) filtered.get("users")).get(0);
        assertEquals("[FILTERED]", user.get("password"));
        assertEquals("Ada", user.get("name"));
        assertTrue(((String) filtered.get("long")).endsWith("[TRUNCATED]"));

        Notice notice = Notice.fromThrowable(
            new RuntimeException("x"),
            new Configuration().setProjectSlug("demo"),
            new NoticeOptions().session(Map.of("auth_token", "t")));
        assertEquals("[FILTERED]", notice.session.get("auth_token"));
    }
}
