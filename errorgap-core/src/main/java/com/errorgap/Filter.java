package com.errorgap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Filter {
    public static final String FILTERED = "[FILTERED]";

    private Filter() {
    }

    public static final String TRUNCATED = "[TRUNCATED]";
    private static final int MAX_STRING = 4096;
    private static final int MAX_ITEMS = 128;
    private static final int MAX_DEPTH = 8;

    /**
     * Mask sensitive keys at any depth (inside lists too) and cut long
     * strings and large or deeply nested collections.
     */
    public static Map<String, Object> params(Map<String, Object> params, List<String> filterKeys) {
        if (params == null || params.isEmpty()) {
            return new LinkedHashMap<>();
        }
        List<String> lowered = filterKeys.stream()
            .map(k -> k.toLowerCase(Locale.ROOT))
            .toList();
        return walkMap(params, lowered, 0);
    }

    public static String string(String value, int limit) {
        return value != null && value.length() > limit ? value.substring(0, limit) + TRUNCATED : value;
    }

    private static Map<String, Object> walkMap(Map<?, ?> in, List<String> lowered, int depth) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : in.entrySet()) {
            if (out.size() >= MAX_ITEMS) {
                break;
            }
            String key = String.valueOf(entry.getKey());
            out.put(key, isSensitive(key, lowered) ? FILTERED : walk(entry.getValue(), lowered, depth + 1));
        }
        return out;
    }

    private static Object walk(Object value, List<String> lowered, int depth) {
        if (value instanceof String text) {
            return string(text, MAX_STRING);
        }
        if (value instanceof Map<?, ?> nested) {
            return depth >= MAX_DEPTH ? TRUNCATED : walkMap(nested, lowered, depth);
        }
        if (value instanceof Iterable<?> items) {
            if (depth >= MAX_DEPTH) {
                return TRUNCATED;
            }
            java.util.ArrayList<Object> out = new java.util.ArrayList<>();
            for (Object item : items) {
                if (out.size() >= MAX_ITEMS) {
                    break;
                }
                out.add(walk(item, lowered, depth + 1));
            }
            return out;
        }
        if (value instanceof Object[] array) {
            return walk(java.util.Arrays.asList(array), lowered, depth);
        }
        return value;
    }

    private static boolean isSensitive(String key, List<String> lowered) {
        if (key == null) {
            return false;
        }
        String lk = key.toLowerCase(Locale.ROOT);
        for (String needle : lowered) {
            if (!needle.isEmpty() && lk.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
