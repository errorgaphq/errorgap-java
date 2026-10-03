package com.errorgap;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionContextTest {
    @Test
    void scopesNestAndRestore() {
        try (TransactionContext.Scope outer = TransactionContext.enter("outer")) {
            try (TransactionContext.Scope inner = TransactionContext.enter("inner")) {
                assertEquals("inner", TransactionContext.current());
            }
            assertEquals("outer", TransactionContext.current());
        }
        assertNull(TransactionContext.current());
    }

    @Test
    void aNoticeInsideAScopeCarriesTheTransactionId() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        try {
            Client client = new Client(new Configuration()
                .setEndpoint(ing.endpoint()).setProjectSlug("demo").setApiKey("egp_test").setAsync(false));
            try {
                ApmTransaction transaction = new ApmTransaction();
                try (TransactionContext.Scope scope = TransactionContext.enter(transaction.getId())) {
                    client.notify(new RuntimeException("boom"));
                    client.notify(new RuntimeException("explicit"),
                        new NoticeOptions().context(Map.of("transaction_id", "mine")));
                }
                client.notify(new RuntimeException("after"));

                // Only the context: backtrace source excerpts quote this test's code.
                var bodies = ing.requests().stream()
                    .map(r -> r.body.substring(r.body.lastIndexOf("\"context\":")))
                    .toList();
                assertTrue(bodies.get(0).contains("\"transaction_id\":\"" + transaction.getId() + "\""), bodies.get(0));
                assertTrue(bodies.get(1).contains("\"transaction_id\":\"mine\""), bodies.get(1));
                assertFalse(bodies.get(2).contains("transaction_id"), bodies.get(2));
                assertTrue(transaction.toMap(new Configuration()).get("id").equals(transaction.getId()));
            } finally {
                client.shutdown(Duration.ofSeconds(2));
            }
        } finally {
            ing.close();
        }
    }
}
