package com.flowbase.engine.realtime.config;

import com.flowbase.engine.realtime.handler.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
class WebSocketConfiguration implements WebSocketConfigurer {
    private final RealtimeWebSocketHandler realtimeWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/v1/realtime").setAllowedOrigins("*");
    }
}
