package com.flowbase.engine.config;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidTokenException;
import com.flowbase.engine.auth.exception.RevokedTokenException;
import com.flowbase.engine.auth.service.JwtService;
import com.flowbase.engine.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
@AllArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (this.isBypassed(request.getRequestURI(), request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer")) token = token.substring(7);
            if (token == null || token.isBlank()) {
                Cookie[] cookies = request.getCookies();
                if (cookies == null || cookies.length == 0) {
                    throw new InvalidTokenException("No token header or cookie found!");
                } for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("fb_access_token")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            if (token == null || token.isBlank()) {
                throw new InvalidTokenException("Access token cookie 'fb_access_token' missing!");
            }
            AuthenticatedUser user = this.jwtService.parseAndValidate(token);
            if (this.tokenBlacklistService.isRevoked(user.jti()))
                throw new RevokedTokenException("This session has been revoked!");
            if (!user.tenantId().equals(TenantContext.get())) {
                throw new AuthenticationException("Token tenant scope does not match the active request context!");
            } UserContext.set(user);
        } catch (AuthenticationException e) {
            ErrorHelper.sendUnAuthorizedError(response, e.getMessage()); return;
        } try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
    
    private boolean isBypassed(String requestURI, String requestMethod) {
        if (requestURI.equals("/v1/auth/register") || requestURI.equals("/v1/auth/login") || requestURI.equals("/v1/auth/refresh") || requestURI.equals("/v1/realtime"))
            return true;
        return requestURI.startsWith("/v1/tenants");
    }
}
