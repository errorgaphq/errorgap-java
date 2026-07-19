package com.errorgap;

import java.util.LinkedHashMap;
import java.util.Map;

public record ApmSpan(
    String kind,
    String sql,
    String file,
    Integer line,
    String function,
    double durationMs
) {
    public static ApmSpan database(String sql,
                                   String file,
                                   Integer line,
                                   String function,
                                   double durationMs) {
        return new ApmSpan("db", sql, file, line, function, durationMs);
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", kind);
        if (sql != null) map.put("sql", sql);
        if (file != null) map.put("file", file);
        if (line != null) map.put("line", line);
        if (function != null) map.put("fn_name", function);
        map.put("duration_ms", durationMs);
        return map;
    }
}
