package com.errorgap.spring;

import com.errorgap.TransactionContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Errors reported during a request or job carry its transaction id. */
class TransactionIdTest {
    @Test
    void anErrorReportedDuringARequestCarriesItsId() throws Exception {
        RecordingClient client = new RecordingClient();
        try {
            ErrorgapWebFilter filter = new ErrorgapWebFilter(client, new QuerySpanCollector());
            filter.doFilter(new MockHttpServletRequest("GET", "/orders/7"), new MockHttpServletResponse(),
                (req, res) -> client.notify(new IllegalStateException("card declined"), null, false));

            String id = client.transactions.get(0).getId();
            assertTrue(id.matches("[0-9a-f-]{36}"), id);
            assertEquals(id, client.transactionIds.get(0));
            assertNull(TransactionContext.current(), "the id does not outlive the request");
        } finally {
            client.close();
        }
    }

    @Test
    void aFailedJobsErrorCarriesItsId() {
        RecordingClient client = new RecordingClient();
        try {
            ErrorgapApm apm = new ErrorgapApm(client, new QuerySpanCollector());
            assertThrows(IllegalStateException.class, () -> apm.trackJob("ReceiptJob", "mail", () -> {
                throw new IllegalStateException("smtp down");
            }));
            assertEquals(client.transactions.get(0).getId(), client.transactionIds.get(0));
            assertNull(TransactionContext.current());
        } finally {
            client.close();
        }
    }
}
