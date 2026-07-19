package com.errorgap;

import java.time.Duration;

/**
 * Package-level façade. {@link #init(Configuration)} installs a default
 * client; {@link #notify(Throwable)} reports against it.
 */
public final class Errorgap {

    private static volatile Client defaultClient;
    private static volatile Configuration configuration;
    private static volatile Thread.UncaughtExceptionHandler previousHandler;
    private static volatile boolean handlersInstalled;

    private Errorgap() {
    }

    public static void init(Configuration cfg) {
        init(cfg, true);
    }

    public static void init(Configuration cfg, boolean captureGlobals) {
        cfg.validate();
        configuration = cfg;
        defaultClient = new Client(cfg);
        if (captureGlobals) {
            installHandlers();
        } else {
            uninstallHandlers();
        }
    }

    public static Configuration configuration() {
        return configuration;
    }

    public static Client client() {
        return defaultClient;
    }

    public static Client.Result notify(Throwable throwable) {
        Client c = defaultClient;
        if (c == null) {
            return new Client.Result(null, null, new IllegalStateException("Errorgap not initialized"), false);
        }
        return c.notify(throwable);
    }

    public static Client.Result notify(Throwable throwable, NoticeOptions options) {
        Client c = defaultClient;
        if (c == null) {
            return new Client.Result(null, null, new IllegalStateException("Errorgap not initialized"), false);
        }
        return c.notify(throwable, options);
    }

    public static Client.Result notifyTransaction(ApmTransaction transaction) {
        Client c = defaultClient;
        if (c == null) {
            return new Client.Result(null, null, new IllegalStateException("Errorgap not initialized"), false);
        }
        return c.notifyTransaction(transaction);
    }

    public static void flush(Duration timeout) throws InterruptedException {
        Client c = defaultClient;
        if (c != null) {
            c.flush(timeout);
        }
    }

    public static void shutdown(Duration timeout) throws InterruptedException {
        Client c = defaultClient;
        if (c != null) {
            c.shutdown(timeout);
            defaultClient = null;
        }
        uninstallHandlers();
    }

    private static synchronized void installHandlers() {
        if (handlersInstalled) {
            return;
        }
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            NoticeOptions opts = new NoticeOptions();
            opts.context = java.util.Map.of(
                "source", "uncaughtExceptionHandler",
                "thread", thread.getName()
            );
            notify(throwable, opts);
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable);
            }
        });
        handlersInstalled = true;
    }

    private static synchronized void uninstallHandlers() {
        if (!handlersInstalled) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        previousHandler = null;
        handlersInstalled = false;
    }
}
