package com.flowbase.engine.realtime.handler;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.service.JwtService;
import com.flowbase.engine.realtime.dto.WsFrame;
import com.flowbase.engine.realtime.service.SubscriptionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket Handshake Successful! Session Opened: {}", session.getId());
//        super.afterConnectionEstablished(session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received frame payload from session {}: {}", session.getId(), message.getPayload());
//        super.handleTextMessage(session, message);
        try {
            String payload = message.getPayload(); WsFrame frame = this.objectMapper.readValue(payload, WsFrame.class);
            if (frame.action() == null || frame.action().isEmpty()) {
                session.sendMessage(new TextMessage("{ \"error\":\"Invalid frame layout. 'action' is required\" }"));
                return;
            } switch (frame.action()) {
                case "authenticate" -> this.handleAuthentication(session, frame);
                case "subscribe" -> this.handleSubscription(session, frame);
                case "unsubscribe" -> this.handleUnSubscription(session, frame);
                default -> this.sendReplyMessage(session, "error", "Unknown action: " + frame.action());
            }
        } catch (Exception e) {
            log.error("Failed to process WebSocket frame from session {}", session.getId(), e);
            this.sendReplyMessage(session, "error", "Invalid JSON or payload format.");
        }
    }
    
    private void handleAuthentication(WebSocketSession session, WsFrame frame) throws Exception {
        if (frame.token() == null || frame.token().isBlank()) {
            this.sendReplyMessage(session, "error", "Authentication token missing!");
            return;
        } try {
            AuthenticatedUser user = this.jwtService.parseAndValidate(frame.token());
            session.getAttributes().put("user", user);
            this.sendReplyMessage(session, "status", "Authenticated", "", user.id());
            log.info("Session {} successfully authenticated user {}", session.getId(), user.email());
        } catch (Exception e) {
            this.sendReplyMessage(session, "error", "Authentication failed: " + e.getMessage());
        }
    }
    
    private void handleSubscription(WebSocketSession session, WsFrame frame) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes().get("user"); if (user == null) {
            this.sendReplyMessage(session, "error", "Unauthorized! You must send an 'authenticate' frame first.");
            return;
        }
        if (frame.collectionId() == null || frame.collectionId().isBlank()) {
            this.sendReplyMessage(session, "error", "collectionId is required to subscribe.");
            return;
        } this.subscriptionRegistryService.subscribe(frame.collectionId(), session);
        this.sendReplyMessage(session, "status", "subscribed", frame.collectionId(), "");
        log.info("Session {} subscribed to collection {}", session.getId(), frame.collectionId());
    }
    
    private void handleUnSubscription(WebSocketSession session, WsFrame frame) throws Exception {
        if (frame.collectionId() == null || frame.collectionId().isBlank()) {
            this.sendReplyMessage(session, "error", "collectionId is required to unsubscribe");
            return;
        } this.subscriptionRegistryService.unSubscribe(frame.collectionId(), session);
        this.sendReplyMessage(session, "status", "unsubscribed", frame.collectionId(), "");
        log.info("Session {} unsubscribed from collection {}", session.getId(), frame.collectionId());
    }
    
    private void sendReplyMessage(WebSocketSession session, String type, String... payload) throws Exception {
        if (type.equals("error")) {
            String errorMsg = payload.length > 0 ? payload[0] : "Unknown error";
            session.sendMessage(new TextMessage("{\"error\":\"" + errorMsg + "\"}"));
        } else if (type.equals("status")) {
            String status = payload.length > 0 ? payload[0] : "";
            String collectionId = payload.length > 1 ? payload[1] : "";
            String userId = payload.length > 2 ? payload[2] : "";
            session.sendMessage(new TextMessage("{\"status\":\"" + status + "\",\"collectionId\":\"" + collectionId + "\",\"userId\":\"" + userId + "\"}"));
        }
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        this.subscriptionRegistryService.removeSession(session);
        log.info("WebSocket Session Closed: {} with Status: {}", session.getId(), status);
//        super.afterConnectionClosed(session, status);
    }
}
