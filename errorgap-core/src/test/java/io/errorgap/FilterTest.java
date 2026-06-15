package io.errorgap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {

    private static final List<String> DEFAULTS = List.of(
        "password", "token", "secret", "api_key", "authorization", "cookie"
    );

    @Test
    void masksFilteredKeys() {
        Map<String, Object> out = Filter.params(Map.of(
            "username", "alice",
            "password", "hunter2",
            "access_token", "x"
        ), DEFAULTS);
        assertEquals("alice", out.get("username"));
        assertEquals("[FILTERED]", out.get("password"));
        assertEquals("[FILTERED]", out.get("access_token"));
    }

    @Test
    void recursesIntoNestedMap() {
        Map<String, Object> out = Filter.params(Map.of(
            "user", Map.of("name", "alice", "api_key", "x")
        ), DEFAULTS);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) out.get("user");
        assertEquals("alice", user.get("name"));
        assertEquals("[FILTERED]", user.get("api_key"));
    }

    @Test
    void caseInsensitive() {
        Map<String, Object> out = Filter.params(Map.of("Authorization", "Bearer xyz"), DEFAULTS);
        assertEquals("[FILTERED]", out.get("Authorization"));
    }
}
