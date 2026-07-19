package com.errorgap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Backtrace {

    private static final int CONTEXT_RADIUS = 6;
    private static final long MAX_SOURCE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_LINE_LENGTH = 400;

    private Backtrace() {
    }

    public static List<Frame> fromThrowable(Throwable t, String rootDirectory) {
        List<Frame> frames = new ArrayList<>();
        int index = 0;
        Throwable current = t;
        while (current != null) {
            for (StackTraceElement element : current.getStackTrace()) {
                String file = classToFile(element.getClassName());
                Integer line = element.getLineNumber() > 0 ? element.getLineNumber() : null;
                frames.add(new Frame(
                    file,
                    line,
                    formatFunction(element),
                    isInApp(element),
                    index++,
                    line == null ? null : sourceFor(element, file, line, rootDirectory)
                ));
            }
            current = current.getCause();
        }
        return frames;
    }

    private static String classToFile(String className) {
        if (className == null || className.isEmpty()) {
            return "Unknown.java";
        }
        int innerClass = className.indexOf('$');
        String sourceClass = innerClass >= 0 ? className.substring(0, innerClass) : className;
        return sourceClass.replace('.', '/') + ".java";
    }

    private static String formatFunction(StackTraceElement element) {
        return element.getClassName() + "." + element.getMethodName();
    }

    private static boolean isInApp(StackTraceElement element) {
        String cls = element.getClassName();
        if (cls == null) {
            return false;
        }
        return !(cls.startsWith("java.") || cls.startsWith("javax.")
            || cls.startsWith("jakarta.") || cls.startsWith("jdk.")
            || cls.startsWith("sun.") || cls.startsWith("com.sun.")
            || cls.startsWith("org.springframework.") || cls.startsWith("org.junit.")
            || cls.startsWith("org.apache.") || cls.startsWith("io.netty.")
            || cls.startsWith("com.zaxxer.hikari.") || cls.startsWith("org.hibernate."));
    }

    private static Source sourceFor(StackTraceElement element,
                                    String sourcePath,
                                    int targetLine,
                                    String rootDirectory) {
        List<String> lines = sourceFromRoot(sourcePath, rootDirectory);
        if (lines == null) {
            lines = sourceFromClassPath(element.getClassName(), sourcePath);
        }
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        int targetIndex = Math.min(Math.max(targetLine - 1, 0), lines.size() - 1);
        int startIndex = Math.max(0, targetIndex - CONTEXT_RADIUS);
        int endIndex = Math.min(lines.size(), targetIndex + CONTEXT_RADIUS + 1);
        List<String> excerpt = new ArrayList<>(endIndex - startIndex);
        for (String line : lines.subList(startIndex, endIndex)) {
            excerpt.add(line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) : line);
        }
        return new Source(startIndex + 1, excerpt);
    }

    private static List<String> sourceFromRoot(String sourcePath, String rootDirectory) {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            return null;
        }
        Path root = Path.of(rootDirectory);
        for (Path candidate : List.of(
            root.resolve("src/main/java").resolve(sourcePath),
            root.resolve("src/test/java").resolve(sourcePath),
            root.resolve(sourcePath)
        )) {
            List<String> lines = readPath(candidate);
            if (lines != null) {
                return lines;
            }
        }
        return null;
    }

    private static List<String> sourceFromClassPath(String className, String sourcePath) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = Backtrace.class.getClassLoader();
        }
        try (InputStream resource = loader.getResourceAsStream(sourcePath)) {
            if (resource != null) {
                return readStream(resource);
            }
        } catch (IOException ignored) {
            // Fall through to a sibling Maven source JAR.
        }

        try {
            Class<?> frameClass = Class.forName(className, false, loader);
            if (frameClass.getProtectionDomain() == null
                || frameClass.getProtectionDomain().getCodeSource() == null) {
                return null;
            }
            URI location = frameClass.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path binary = Path.of(location);
            String filename = binary.getFileName() == null ? "" : binary.getFileName().toString();
            if (!filename.endsWith(".jar")) {
                return null;
            }
            Path sourceJar = binary.resolveSibling(
                filename.substring(0, filename.length() - 4) + "-sources.jar"
            );
            if (!Files.isRegularFile(sourceJar) || Files.size(sourceJar) > 100 * 1024 * 1024) {
                return null;
            }
            try (ZipFile zip = new ZipFile(sourceJar.toFile())) {
                ZipEntry entry = zip.getEntry(sourcePath);
                if (entry == null || entry.getSize() > MAX_SOURCE_BYTES) {
                    return null;
                }
                try (InputStream source = zip.getInputStream(entry)) {
                    return readStream(source);
                }
            }
        } catch (ReflectiveOperationException | IOException | java.net.URISyntaxException
                 | IllegalArgumentException | SecurityException ignored) {
            return null;
        }
    }

    private static List<String> readPath(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_SOURCE_BYTES) {
                return null;
            }
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    private static List<String> readStream(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes((int) MAX_SOURCE_BYTES + 1);
        if (bytes.length > MAX_SOURCE_BYTES) {
            return null;
        }
        return Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split("\\R", -1));
    }

    public record Source(int startLine, List<String> lines) {
    }

    public record Frame(
        String file,
        Integer line,
        String function,
        boolean inApp,
        int index,
        Source source
    ) {
    }
}
