package com.errorgap;

import java.util.Map;

public final class NoticeOptions {
    public Map<String, Object> context;
    public Map<String, Object> environment;
    public Map<String, Object> session;
    public Map<String, Object> params;

    public NoticeOptions context(Map<String, Object> v) { this.context = v; return this; }
    public NoticeOptions environment(Map<String, Object> v) { this.environment = v; return this; }
    public NoticeOptions session(Map<String, Object> v) { this.session = v; return this; }
    public NoticeOptions params(Map<String, Object> v) { this.params = v; return this; }
}
