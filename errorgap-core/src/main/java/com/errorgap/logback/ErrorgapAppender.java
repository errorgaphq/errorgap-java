package com.errorgap.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.errorgap.Errorgap;
import com.errorgap.NoticeOptions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logback appender: report error-level log events to Errorgap.
 *
 * <pre>{@code
 * <appender name="ERRORGAP" class="com.errorgap.logback.ErrorgapAppender"/>
 * <root level="INFO"><appender-ref ref="ERRORGAP"/></root>
 * }</pre>
 *
 * {@code log.error("…", exception)} reports that exception. An error event
 * with no exception is reported as a {@link LoggedError} carrying the
 * message. Events below {@code minimumLevel} (default ERROR) are ignored, as
 * are the SDK's own. Needs {@code Errorgap.init(...)} to have run; until then
 * events are dropped.
 */
public class ErrorgapAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    /** An error-level log event that had no exception attached. */
    public static final class LoggedError extends RuntimeException {
        public LoggedError(String message) {
            super(message);
        }
    }

    private Level minimumLevel = Level.ERROR;

    public void setMinimumLevel(String level) {
        this.minimumLevel = Level.toLevel(level, Level.ERROR);
    }

    @Override
    protected void append(ILoggingEvent event) {
        try {
            if (!event.getLevel().isGreaterOrEqual(minimumLevel)) {
                return;
            }
            String logger = event.getLoggerName();
            if (logger != null && logger.startsWith("com.errorgap")) {
                return;
            }

            Map<String, Object> context = new LinkedHashMap<>();
            context.put("source", "logback");
            context.put("component", logger);
            context.put("severity", event.getLevel().toString().toLowerCase(java.util.Locale.ROOT));
            context.put("thread", event.getThreadName());

            Throwable throwable = null;
            IThrowableProxy proxy = event.getThrowableProxy();
            if (proxy instanceof ThrowableProxy real) {
                throwable = real.getThrowable();
            }
            if (throwable != null) {
                context.put("log_message", event.getFormattedMessage());
            } else {
                throwable = new LoggedError(event.getFormattedMessage());
            }

            Map<String, Object> params = new LinkedHashMap<>();
            try {
                Map<String, String> mdc = event.getMDCPropertyMap();
                if (mdc != null) {
                    params.putAll(mdc);
                }
            } catch (RuntimeException noMdc) {
                // A logger context without an MDC adapter: report without it.
            }
            Errorgap.notify(throwable, new NoticeOptions().context(context).params(params));
        } catch (Throwable ignored) {
            // An appender must never break the code that logged.
        }
    }
}
