package com.errorgap.spring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorgapApmTest {
    @Test
    void recordsFailedJobAndReportsException() {
        RecordingClient client = new RecordingClient();
        try {
            ErrorgapApm apm = new ErrorgapApm(client, new QuerySpanCollector());
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                apm.trackJob("example.ReceiptJob", "receipts", () -> {
                    throw new IllegalStateException("receipt failed");
                })
            );

            assertEquals("receipt failed", thrown.getMessage());
            assertEquals(1, client.errors.size());
            assertEquals(1, client.transactions.size());
            assertEquals("job", client.transactions.get(0).getKind());
            assertEquals("example.ReceiptJob", client.transactions.get(0).getJobClass());
            assertEquals(500, client.transactions.get(0).getStatusCode());
        } finally {
            client.close();
        }
    }
}
