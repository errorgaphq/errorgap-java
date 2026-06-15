package io.errorgap;

import java.util.ArrayList;
import java.util.List;

public final class Backtrace {

    public static List<Frame> fromThrowable(Throwable t, String rootDirectory) {
        List<Frame> frames = new ArrayList<>();
        int index = 0;
        Throwable current = t;
        while (current != null) {
            for (StackTraceElement element : current.getStackTrace()) {
                frames.add(new Frame(
                    classToFile(element.getClassName()),
                    element.getLineNumber() > 0 ? element.getLineNumber() : null,
                    formatFunction(element),
                    isInApp(element, rootDirectory),
                    index++
                ));
            }
            current = current.getCause();
        }
        return frames;
    }

    private static String classToFile(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        return className.replace('.', '/') + ".java";
    }

    private static String formatFunction(StackTraceElement element) {
        return element.getClassName() + "." + element.getMethodName();
    }

    private static boolean isInApp(StackTraceElement element, String rootDirectory) {
        String cls = element.getClassName();
        if (cls == null) {
            return false;
        }
        if (cls.startsWith("java.") || cls.startsWith("javax.")
            || cls.startsWith("jdk.") || cls.startsWith("sun.")
            || cls.startsWith("com.sun.")) {
            return false;
        }
        if (rootDirectory == null) {
            return !cls.startsWith("org.junit.");
        }
        // For JVM apps we can't tell solely from class name; treat user
        // classes as in-app by excluding common framework prefixes.
        return !cls.startsWith("org.springframework.")
            && !cls.startsWith("org.junit.")
            && !cls.startsWith("org.apache.")
            && !cls.startsWith("io.netty.");
    }

    public record Frame(
        String file,
        Integer line,
        String function,
        boolean inApp,
        int index
    ) {}
}
