package com.flowbase.engine.tenant.filter;

import com.flowbase.engine.config.ErrorHelper;
import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.repository.TenantRepository;
import com.flowbase.engine.tenant.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Order(1)
@AllArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private final TenantService tenantService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-FlowBase-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            ErrorHelper.sendUnAuthorizedError(response, "Tenant api key missing!");
            return;
        } Optional<Tenant> tenantExists = this.tenantService.findTenantByApiKey(apiKey);
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
