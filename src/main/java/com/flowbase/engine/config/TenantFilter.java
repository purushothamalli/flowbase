package com.flowbase.engine.config;

import com.flowbase.engine.auth.domain.Tenant;
import com.flowbase.engine.auth.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@AllArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private final TenantRepository tenantRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-FlowBase-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            ErrorHelper.sendUnAuthorizedError(response, "Tenant api key missing!");
            return;
        } Optional<Tenant> tenantExists = this.tenantRepository.findByApiKey(apiKey);
        if (tenantExists.isEmpty()) {
            ErrorHelper.sendUnAuthorizedError(response, "Invalid Tenant API key!");
            return;
        }
        TenantContext.set(tenantExists.get().id());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
