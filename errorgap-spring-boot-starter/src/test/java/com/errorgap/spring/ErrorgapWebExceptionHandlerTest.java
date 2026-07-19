package com.errorgap.spring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.jupiter.api.Assertions.*;

class ErrorgapWebExceptionHandlerTest {
    @Test
    void capturesRouteRequestMetadataAndFilteredParameters() {
        RecordingClient client = new RecordingClient();
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/orders/42");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/orders/{id}");
            request.addParameter("customer_id", "cus_42");
            request.addParameter("password", "secret");
            request.addHeader("user-agent", "JUnit");
            ErrorgapWebExceptionHandler handler = new ErrorgapWebExceptionHandler(client);

            assertNull(handler.resolveException(
                request,
                new MockHttpServletResponse(),
                new Object(),
                new IllegalStateException("boom")
            ));

            assertEquals(1, client.errors.size());
            assertEquals("/orders/{id}", client.options.get(0).context.get("route"));
            assertEquals("cus_42", client.options.get(0).params.get("customer_id"));
            assertEquals("secret", client.options.get(0).params.get("password"));
        } finally {
            client.close();
        }
    }
}
