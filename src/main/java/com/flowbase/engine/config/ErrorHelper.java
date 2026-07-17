package com.flowbase.engine.config;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ErrorHelper {
    public static void sendUnAuthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String payload = String.format("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"%s\"}", message);
        response.getWriter().write(payload);
    }
}
