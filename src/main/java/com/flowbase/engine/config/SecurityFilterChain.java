package com.flowbase.engine.config;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.auth.exception.InvalidTokenException;
import com.flowbase.engine.auth.exception.RevokedTokenException;
import com.flowbase.engine.auth.service.JwtService;
import com.flowbase.engine.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class SecurityFilterChain extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().equals("/v1/auth/register") || request.getRequestURI().equals("/v1/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer")) token = token.substring(7);
        if (token == null || token.isBlank()) throw new InvalidTokenException("Invalid token!");
        try {
            AuthenticatedUser user = this.jwtService.parseAndValidate(token);
            if (this.tokenBlacklistService.isRevoked(user.jti()))
                throw new RevokedTokenException("This session has been revoked!");
            UserContext.set(user);
        } catch (AuthenticationException e) {
            this.sendUnAuthorizedError(response, e.getMessage());
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
    
    private void sendUnAuthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String payload = String.format("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"%s\"}", message);
        response.getWriter().write(payload);
    }
}
