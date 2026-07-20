package com.flowbase.engine.realtime.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket Handshake Successful! Session Opened: {}", session.getId());
//        super.afterConnectionEstablished(session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received frame payload from session {}: {}", session.getId(), message.getPayload());
//        super.handleTextMessage(session, message);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket Session Closed: {} with Status: {}", session.getId(), status);
//        super.afterConnectionClosed(session, status);
    }
}
