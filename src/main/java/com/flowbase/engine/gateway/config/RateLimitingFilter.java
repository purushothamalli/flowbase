package com.flowbase.engine.gateway.config;

import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.gateway.dto.RateLimitResult;
import com.flowbase.engine.gateway.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/v1/tenants") || path.startsWith("/v1/auth/register") || path.startsWith("/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String tenantId = TenantContext.get();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader("X-FlowBase-API-Key");
        }
        if (tenantId != null && !tenantId.isBlank()) {
            RateLimitResult rateLimitResult = this.rateLimiterService.checkRateLimit(tenantId,
                    this.rateLimitProperties.getDefaultCapacity(), this.rateLimitProperties.getRefillRate(), 1);
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitResult.capacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.remainingTokens()));
            if (!rateLimitResult.allowed()) {
                response.setStatus(429);
                response.setHeader("Retry-After", "1");
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\": \"Rate limit exceeded. Too many requests.\", \"retryAfterSeconds\": 1}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
