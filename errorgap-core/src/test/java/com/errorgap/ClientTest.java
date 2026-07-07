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
}
