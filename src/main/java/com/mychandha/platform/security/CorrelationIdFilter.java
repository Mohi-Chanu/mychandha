package com.mychandha.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"api", "local", "test"})
public final class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_PARENT_HEADER = "traceparent";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");
    private static final Pattern SAFE_TRACE_PARENT =
            Pattern.compile("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
    private static final String ZERO_TRACE_ID = "0".repeat(32);
    private static final String ZERO_SPAN_ID = "0".repeat(16);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = safeCorrelationId(request.getHeader(HEADER));
        String requestId = UUID.randomUUID().toString();
        String traceParent = safeTraceParent(request.getHeader(TRACE_PARENT_HEADER));
        response.setHeader(HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try (MDC.MDCCloseable correlation = MDC.putCloseable("correlationId", correlationId);
             MDC.MDCCloseable requestContext = MDC.putCloseable("requestId", requestId);
             MDC.MDCCloseable traceContext = MDC.putCloseable("traceparent", traceParent);
             MDC.MDCCloseable trace = MDC.putCloseable("traceId", traceId(traceParent));
             MDC.MDCCloseable span = MDC.putCloseable("spanId", spanId(traceParent))) {
            filterChain.doFilter(request, response);
        }
    }

    static String safeCorrelationId(String supplied) {
        return supplied != null && SAFE_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }

    static String safeTraceParent(String supplied) {
        if (supplied != null
                && SAFE_TRACE_PARENT.matcher(supplied).matches()
                && !ZERO_TRACE_ID.equals(traceId(supplied))
                && !ZERO_SPAN_ID.equals(spanId(supplied))) {
            return supplied;
        }
        return "00-" + uuidHex() + "-" + uuidHex().substring(0, 16) + "-01";
    }

    private static String traceId(String traceParent) {
        return traceParent.substring(3, 35);
    }

    private static String spanId(String traceParent) {
        return traceParent.substring(36, 52);
    }

    private static String uuidHex() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
