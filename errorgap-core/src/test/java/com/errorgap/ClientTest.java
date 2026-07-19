package com.errorgap;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    @Test
    void postsToNoticesWithCanonicalHeaders() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Configuration cfg = new Configuration()
                .setEndpoint(ing.endpoint())
                .setProjectSlug("demo")
                .setApiKey("flk_test")
                .setAsync(false);
            Client client = new Client(cfg);
            try {
                Client.Result result = client.notify(new RuntimeException("test"));
                assertTrue(result.success());

                List<FakeIngestor.CapturedRequest> reqs = ing.requests();
                assertEquals(1, reqs.size());
                FakeIngestor.CapturedRequest req = reqs.get(0);
                assertEquals("POST", req.method);
                assertEquals("/api/projects/demo/notices", req.path);
                assertEquals("application/json", req.headers.get("content-type"));
                assertEquals("flk_test", req.headers.get("x-errorgap-project-key"));
                assertTrue(req.headers.get("user-agent").startsWith("errorgap-java/"));
            } finally {
                client.shutdown(Duration.ofSeconds(2));
            }
        } finally {
            ing.close();
        }
    }

    @Test
    void asyncQueuesAndFlushes() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Configuration cfg = new Configuration()
                .setEndpoint(ing.endpoint())
                .setProjectSlug("demo")
                .setApiKey("flk_test")
                .setAsync(true);
            Client client = new Client(cfg);
            try {
                Client.Result result = client.notify(new RuntimeException("x"));
                assertTrue(result.queued);
                assertEquals(202, result.status);

                client.flush(Duration.ofSeconds(5));
                assertEquals(1, ing.requests().size());
            } finally {
                client.shutdown(Duration.ofSeconds(2));
            }
        } finally {
            ing.close();
        }
    }

    @Test
    void rejectsMissingProjectSlug() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Configuration cfg = new Configuration().setEndpoint(ing.endpoint());
            Client client = new Client(cfg);
            try {
                Client.Result result = client.notify(new RuntimeException("x"));
                assertNotNull(result.error);
                assertTrue(ing.requests().isEmpty());
            } finally {
                client.shutdown(Duration.ofSeconds(2));
            }
        } finally {
            ing.close();
        }
    }

    @Test
    void postsApmTransactionsWhenEnabled() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Configuration cfg = new Configuration()
                .setEndpoint(ing.endpoint())
                .setProjectSlug("demo")
                .setApiKey("flk_test")
                .setAsync(false)
                .setApmEnabled(true);
            Client client = new Client(cfg);
            try {
                Client.Result result = client.notifyTransaction(new ApmTransaction()
                    .setKind("web")
                    .setMethod("GET")
                    .setPath("/orders/{id}")
                    .setStatusCode(200)
                    .setDurationMs(12.5)
                    .addSpan(ApmSpan.database("select ?", "Order.java", 42, "find", 2.5)));
                assertTrue(result.success());
                FakeIngestor.CapturedRequest req = ing.requests().get(0);
                assertEquals("/api/projects/demo/transactions", req.path);
                assertTrue(req.body.contains("\"duration_ms\":12.5"));
                assertTrue(req.body.contains("\"kind\":\"db\""));
            } finally {
                client.close();
            }
        } finally {
            ing.close();
        }
    }

    @Test
    void skipsApmTransactionsWhenDisabled() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Client client = new Client(new Configuration()
                .setEndpoint(ing.endpoint())
                .setProjectSlug("demo")
                .setApmEnabled(false));
            try {
                assertEquals(204, client.notifyTransaction(new ApmTransaction()).status);
                assertTrue(ing.requests().isEmpty());
            } finally {
                client.close();
            }
        } finally {
            ing.close();
        }
    }
}
