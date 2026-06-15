package io.errorgap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Filter {
    public static final String FILTERED = "[FILTERED]";

    private Filter() {
    }

    public static Map<String, Object> params(Map<String, Object> params, List<String> filterKeys) {
        if (params == null || params.isEmpty()) {
            return new LinkedHashMap<>();
        }
        List<String> lowered = filterKeys.stream()
            .map(k -> k.toLowerCase(Locale.ROOT))
            .toList();
        return walk(params, lowered);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> walk(Map<String, Object> in, List<String> lowered) {
        Map<String, Object> out = new LinkedHashMap<>(in.size());
        for (Map.Entry<String, Object> entry : in.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitive(key, lowered)) {
                out.put(key, FILTERED);
            } else if (value instanceof Map<?, ?> nested) {
                out.put(key, walk((Map<String, Object>) nested, lowered));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    private static boolean isSensitive(String key, List<String> lowered) {
        if (key == null) {
            return false;
        }
        String lk = key.toLowerCase(Locale.ROOT);
        for (String needle : lowered) {
            if (lk.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
