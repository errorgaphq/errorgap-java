package com.errorgap;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON encoder. Lets us avoid pulling in Jackson/Gson as a
 * runtime dependency. Handles only the types used in notice envelopes:
 * Map&lt;String,Object&gt;, List, String, Number, Boolean, null.
 */
final class Json {

    private Json() {
    }

    static String encode(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof Map<?, ?> m) {
            writeMap(sb, m);
            return;
        }
        if (value instanceof List<?> l) {
            writeList(sb, l);
            return;
        }
        if (value instanceof CharSequence s) {
            writeString(sb, s.toString());
            return;
        }
        if (value instanceof Boolean b) {
            sb.append(b);
            return;
        }
        if (value instanceof Number n) {
            sb.append(n);
            return;
        }
        // Fallback: stringify with toString().
        writeString(sb, String.valueOf(value));
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            write(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            write(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
