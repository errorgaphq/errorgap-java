package com.errorgap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.errorgap.logback.ErrorgapAppender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogbackAppenderTest {

    @Test
    void reportsErrorEventsWithOrWithoutAnException() throws Exception {
        FakeIngestor ing = new FakeIngestor();
        Errorgap.init(new Configuration().setEndpoint(ing.endpoint()).setProjectSlug("demo").setAsync(false), false);
        LoggerContext context = new LoggerContext();
        ErrorgapAppender appender = new ErrorgapAppender();
        appender.setContext(context);
        appender.start();
        Logger logger = context.getLogger("billing.Invoices");
        logger.addAppender(appender);
        try {
            logger.warn("below the level: not reported");
            logger.error("could not charge order {}", 7, new IllegalStateException("card declined"));
            logger.error("ledger out of balance");

            List<FakeIngestor.CapturedRequest> requests = ing.requests();
            assertEquals(2, requests.size());
            assertTrue(requests.get(0).body.contains("\"type\":\"IllegalStateException\""));
            assertTrue(requests.get(0).body.contains("\"log_message\":\"could not charge order 7\""));
            assertTrue(requests.get(0).body.contains("\"component\":\"billing.Invoices\""));
            assertTrue(requests.get(1).body.contains("LoggedError"));
            assertTrue(requests.get(1).body.contains("ledger out of balance"));
        } finally {
            appender.stop();
            Errorgap.shutdown(java.time.Duration.ofSeconds(1));
            ing.close();
        }
    }
}
