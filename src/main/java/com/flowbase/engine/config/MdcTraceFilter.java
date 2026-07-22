package com.flowbase.engine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(0)
public class MdcTraceFilter extends OncePerRequestFilter {
    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceparent = request.getHeader(TRACEPARENT_HEADER);
        String traceId, spanId;
        if (traceparent != null && traceparent.startsWith("00-") && traceparent.split("-").length >= 4) {
            String[] parts = traceparent.split("-");
            traceId = parts[1];
            spanId = parts[2];
        } else {
            traceId = UUID.randomUUID()
                          .toString()
                          .replace("-", "");
            spanId = UUID.randomUUID()
                         .toString()
                         .replace("-", "")
                         .substring(0, 16);
            traceparent = String.format("00-%s-%s-01", traceId, spanId);
        }
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_SPAN_ID, spanId);
        response.setHeader(TRACEPARENT_HEADER, traceparent);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
        }
    }
}
