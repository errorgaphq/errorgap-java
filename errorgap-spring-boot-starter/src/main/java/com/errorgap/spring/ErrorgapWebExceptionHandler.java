package com.errorgap.spring;

import com.errorgap.Client;
import com.errorgap.NoticeOptions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring MVC handler exception resolver that reports unhandled exceptions
 * to Errorgap. Runs at the highest precedence so it sees the exception
 * before any built-in resolver translates it into a response.
 *
 * Returning {@code null} delegates rendering to the next resolver in the
 * chain — typically Spring's default error response.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorgapWebExceptionHandler implements HandlerExceptionResolver {

    private final Client client;

    public ErrorgapWebExceptionHandler(Client client) {
        this.client = client;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        NoticeOptions opts = new NoticeOptions();
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("source", "spring.HandlerExceptionResolver");
        ctx.put("url", request.getRequestURL().toString());
        ctx.put("component", request.getRequestURI());
        ctx.put("action", request.getMethod());
        opts.context = ctx;

        Map<String, Object> env = new LinkedHashMap<>();
        env.put("method", request.getMethod());
        env.put("path", request.getRequestURI());
        env.put("query_string", request.getQueryString());
        env.put("user_agent", request.getHeader("user-agent"));
        env.put("remote_addr", request.getRemoteAddr());
        opts.environment = env;

        client.notify(ex, opts, true);

        // Return null so the next exception resolver handles rendering.
        return null;
    }
}
