package com.errorgap.spring;

import com.errorgap.ApmTransaction;
import com.errorgap.Client;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class ErrorgapWebFilter extends OncePerRequestFilter {
    private final Client client;
    private final QuerySpanCollector spans;

    public ErrorgapWebFilter(Client client, QuerySpanCollector spans) {
        this.client = client;
        this.spans = spans;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {
        long started = System.nanoTime();
        Throwable failure = null;
        spans.begin();
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException | Error caught) {
            failure = caught;
            throw caught;
        } finally {
            List<com.errorgap.ApmSpan> querySpans = spans.finish();
            Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String normalizedPath = bestPattern == null
                ? request.getRequestURI()
                : String.valueOf(bestPattern);
            int status = failure == null ? response.getStatus() : 500;
            client.notifyTransaction(new ApmTransaction()
                .setKind("web")
                .setMethod(request.getMethod())
                .setPath(normalizedPath)
                .setPathRaw(request.getRequestURI())
                .setStatusCode(status)
                .setDurationMs((System.nanoTime() - started) / 1_000_000.0)
                .setSpans(querySpans));
        }
    }
}
