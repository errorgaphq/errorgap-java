package com.errorgap.spring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.*;

class ErrorgapWebFilterTest {
    @Test
    void recordsNormalizedRequestTransaction() throws Exception {
        RecordingClient client = new RecordingClient();
        try {
            QuerySpanCollector spans = new QuerySpanCollector();
            ErrorgapWebFilter filter = new ErrorgapWebFilter(client, spans);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders/42");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> {
                req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/orders/{id}");
                ((MockHttpServletResponse) res).setStatus(201);
            });

            assertEquals(1, client.transactions.size());
            assertEquals("web", client.transactions.get(0).getKind());
            assertEquals("/orders/{id}", client.transactions.get(0).getPath());
            assertEquals("/orders/42", client.transactions.get(0).getPathRaw());
            assertEquals(201, client.transactions.get(0).getStatusCode());
        } finally {
            client.close();
        }
    }

    @Test
    void recordsTheBrowserTraceHeader() throws Exception {
        RecordingClient client = new RecordingClient();
        try {
            ErrorgapWebFilter filter = new ErrorgapWebFilter(client, new QuerySpanCollector());
            MockHttpServletRequest traced = new MockHttpServletRequest("GET", "/orders/42");
            traced.addHeader("X-Errorgap-Trace", "0192F3C4-7A1B-4C2D-9E3F-0123456789AB");
            filter.doFilter(traced, new MockHttpServletResponse(), (req, res) -> { });
            MockHttpServletRequest malformed = new MockHttpServletRequest("GET", "/orders/43");
            malformed.addHeader("X-Errorgap-Trace", "not-a-uuid");
            filter.doFilter(malformed, new MockHttpServletResponse(), (req, res) -> { });

            assertEquals("0192f3c4-7a1b-4c2d-9e3f-0123456789ab", client.transactions.get(0).getTraceId());
            assertNotEquals(client.transactions.get(0).getId(), client.transactions.get(0).getTraceId());
            assertNull(client.transactions.get(1).getTraceId());
        } finally {
            client.close();
        }
    }
}
