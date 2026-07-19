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
}
