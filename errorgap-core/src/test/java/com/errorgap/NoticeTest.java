package com.errorgap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoticeTest {

    @Test
    void capturesTypeAndMessage() {
        Configuration cfg = new Configuration().setProjectSlug("demo");
        Notice notice = Notice.fromThrowable(new IllegalArgumentException("boom"), cfg, new NoticeOptions());
        Map<String, Object> err = notice.errors.get(0);
        assertEquals("IllegalArgumentException", err.get("type"));
        assertEquals("boom", err.get("message"));
    }

    @Test
    void includesNotifierIdentification() {
        Configuration cfg = new Configuration().setProjectSlug("demo").setEnvironment("test").setRelease("1.2.3");
        Notice notice = Notice.fromThrowable(new RuntimeException("x"), cfg, new NoticeOptions());
        assertEquals("errorgap-java", notice.context.get("notifier"));
        assertEquals(Version.VERSION, notice.context.get("notifier_version"));
        assertEquals("test", notice.context.get("environment"));
        assertEquals("1.2.3", notice.context.get("release"));
    }

    @Test
    void filtersSensitiveParams() {
        Configuration cfg = new Configuration().setProjectSlug("demo");
        NoticeOptions opts = new NoticeOptions().params(Map.of(
            "username", "alice",
            "password", "hunter2"
        ));
        Notice notice = Notice.fromThrowable(new RuntimeException("x"), cfg, opts);
        assertEquals("[FILTERED]", notice.params.get("password"));
        assertEquals("alice", notice.params.get("username"));
    }

    @Test
    void includesProjectId() {
        Configuration cfg = new Configuration().setProjectSlug("demo").setProjectId("p_1");
        Notice notice = Notice.fromThrowable(new RuntimeException("x"), cfg, new NoticeOptions());
        Map<String, Object> map = notice.toMap();
        assertEquals("p_1", map.get("project_id"));
    }

    @Test
    void backtraceIsPopulated() {
        Configuration cfg = new Configuration().setProjectSlug("demo");
        Notice notice = Notice.fromThrowable(new RuntimeException("x"), cfg, new NoticeOptions());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> frames = (List<Map<String, Object>>) notice.errors.get(0).get("backtrace");
        assertFalse(frames.isEmpty());
    }
}
