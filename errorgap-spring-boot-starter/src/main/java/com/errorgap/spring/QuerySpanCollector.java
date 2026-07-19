package com.errorgap.spring;

import com.errorgap.ApmSpan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class QuerySpanCollector {
    private final ThreadLocal<Deque<List<ApmSpan>>> spans =
        ThreadLocal.withInitial(ArrayDeque::new);

    public void begin() {
        spans.get().push(new ArrayList<>());
    }

    public List<ApmSpan> finish() {
        Deque<List<ApmSpan>> stack = spans.get();
        if (stack.isEmpty()) {
            return List.of();
        }
        List<ApmSpan> result = List.copyOf(stack.pop());
        if (stack.isEmpty()) {
            spans.remove();
        }
        return result;
    }

    public void recordDatabase(String sql, double durationMs) {
        Deque<List<ApmSpan>> stack = spans.get();
        if (stack.isEmpty()) {
            return;
        }
        Callsite callsite = applicationCallsite();
        stack.peek().add(ApmSpan.database(
            normalizeSql(sql),
            callsite == null ? null : callsite.file(),
            callsite == null ? null : callsite.line(),
            callsite == null ? null : callsite.function(),
            durationMs
        ));
    }

    public static String normalizeSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql
            .replaceAll("'(?:''|[^'])*'", "?")
            .replaceAll("\\b\\d+(?:\\.\\d+)?\\b", "?")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static Callsite applicationCallsite() {
        return applicationCallsite(Thread.currentThread().getStackTrace());
    }

    static Callsite applicationCallsite(StackTraceElement[] frames) {
        for (StackTraceElement frame : frames) {
            String cls = frame.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("jdk.")
                || cls.startsWith("jakarta.") || cls.startsWith("javax.")
                || cls.startsWith("com.errorgap.spring.") || cls.startsWith("org.springframework.")
                || cls.startsWith("com.zaxxer.hikari.") || cls.startsWith("org.h2.")
                || cls.startsWith("org.hibernate.")) {
                continue;
            }
            String sourceClass = cls.contains("$") ? cls.substring(0, cls.indexOf('$')) : cls;
            return new Callsite(
                "src/main/java/" + sourceClass.replace('.', '/') + ".java",
                frame.getLineNumber() > 0 ? frame.getLineNumber() : null,
                cls + "." + frame.getMethodName()
            );
        }
        return null;
    }

    record Callsite(String file, Integer line, String function) {
    }
}
