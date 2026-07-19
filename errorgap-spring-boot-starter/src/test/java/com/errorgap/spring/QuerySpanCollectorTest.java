package com.errorgap.spring;

import com.errorgap.ApmSpan;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuerySpanCollectorTest {
    @Test
    void normalizesSqlLiterals() {
        assertEquals(
            "SELECT ? AS inventory WHERE ? = ? AND name = ?",
            QuerySpanCollector.normalizeSql(
                "SELECT 42 AS inventory WHERE 1 = 1 AND name = 'widget'"
            )
        );
    }

    @Test
    void attributesQueriesToApplicationPackagesThatShareTheErrorgapNamespace() {
        QuerySpanCollector.Callsite callsite = QuerySpanCollector.applicationCallsite(
            new StackTraceElement[] {
                new StackTraceElement(
                    "com.errorgap.spring.ErrorgapDataSourceBeanPostProcessor$StatementHandler",
                    "invoke", "ErrorgapDataSourceBeanPostProcessor.java", 182
                ),
                new StackTraceElement(
                    "org.springframework.jdbc.core.JdbcTemplate",
                    "query", "JdbcTemplate.java", 734
                ),
                new StackTraceElement(
                    "com.errorgap.example.SdkController",
                    "order", "SdkController.java", 50
                ),
                new StackTraceElement(
                    "jakarta.servlet.http.HttpServlet",
                    "service", "HttpServlet.java", 590
                ),
            }
        );

        assertNotNull(callsite);
        assertEquals("src/main/java/com/errorgap/example/SdkController.java", callsite.file());
        assertEquals(50, callsite.line());
        assertEquals("com.errorgap.example.SdkController.order", callsite.function());
    }

    @Test
    void dataSourceProxyRecordsQuerySpans() throws Exception {
        JdbcDataSource raw = new JdbcDataSource();
        raw.setURL("jdbc:h2:mem:errorgap;DB_CLOSE_DELAY=-1");
        QuerySpanCollector collector = new QuerySpanCollector();
        DataSource wrapped = (DataSource) new ErrorgapDataSourceBeanPostProcessor(() -> collector)
            .postProcessAfterInitialization(raw, "dataSource");

        collector.begin();
        try (Connection connection = wrapped.getConnection();
             Statement statement = connection.createStatement();
             ResultSet ignored = statement.executeQuery("SELECT 42 AS inventory WHERE 1 = 1")) {
            assertTrue(ignored.next());
        }
        List<ApmSpan> spans = collector.finish();

        assertEquals(1, spans.size());
        assertEquals("SELECT ? AS inventory WHERE ? = ?", spans.get(0).sql());
        assertEquals("db", spans.get(0).kind());
        assertTrue(spans.get(0).durationMs() >= 0);
    }
}
