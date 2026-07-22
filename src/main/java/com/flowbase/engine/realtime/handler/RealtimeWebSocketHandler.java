package com.flowbase.engine.realtime.handler;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.auth.service.JwtService;
import com.flowbase.engine.realtime.dto.WsFrame;
import com.flowbase.engine.realtime.dto.WsResponse;
import com.flowbase.engine.realtime.service.SubscriptionRegistryService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RealtimeWebSocketHandler extends TextWebSocketHandler {
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    public RealtimeWebSocketHandler(SubscriptionRegistryService subscriptionRegistryService, JwtService jwtService, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.subscriptionRegistryService = subscriptionRegistryService;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        meterRegistry.gauge("flowbase_realtime_connections", activeConnections);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket Handshake Successful! Session Opened: {}", session.getId());
        this.activeConnections.incrementAndGet();
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received frame payload from session {}: {}", session.getId(), message.getPayload());
//        super.handleTextMessage(session, message);
        try {
            String payload = message.getPayload(); WsFrame frame = this.objectMapper.readValue(payload, WsFrame.class);
            if (frame.action() == null) {
                this.sendReplyMessage(session, WsResponse.error("Invalid frame layout. 'action' is required")); return;
            } switch (frame.action()) {
                case AUTHENTICATE -> this.handleAuthentication(session, frame);
                case SUBSCRIBE -> this.handleSubscription(session, frame);
                case UNSUBSCRIBE -> this.handleUnSubscription(session, frame);
                default -> this.sendReplyMessage(session, WsResponse.error("Unknown action: " + frame.action()));
            }
        } catch (Exception e) {
            log.error("Failed to process WebSocket frame from session {}", session.getId(), e);
            this.sendReplyMessage(session, WsResponse.error("Invalid JSON or payload format."));
        }
    }
    
    private void handleAuthentication(WebSocketSession session, WsFrame frame) throws Exception {
        if (frame.token() == null || frame.token()
                                          .isBlank()) {
            this.sendReplyMessage(session, WsResponse.error("Authentication token missing!")); return;
        } try {
            AuthenticatedUser user = this.jwtService.parseAndValidate(frame.token());
            session.getAttributes()
                   .put("user", user);
            this.sendReplyMessage(session, WsResponse.status("Authenticated", null, user.id()));
            log.info("Session {} successfully authenticated user {}", session.getId(), user.email());
        } catch (Exception e) {
            this.sendReplyMessage(session, WsResponse.error("Authentication failed: " + e.getMessage()));
        }
    }
    
    private void handleSubscription(WebSocketSession session, WsFrame frame) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                                                            .get("user"); if (user == null) {
            this.sendReplyMessage(session, WsResponse.error("Unauthorized! You must send an 'authenticate' frame " + "first."));
            return;
        } if (frame.collectionId() == null || frame.collectionId()
                                                   .isBlank()) {
            this.sendReplyMessage(session, WsResponse.error("collectionId is required to subscribe.")); return;
        } this.subscriptionRegistryService.subscribe(frame.collectionId(), session);
        this.sendReplyMessage(session, WsResponse.status("subscribed", frame.collectionId(), null));
        log.info("Session {} subscribed to collection {}", session.getId(), frame.collectionId());
    }
    
    private void handleUnSubscription(WebSocketSession session, WsFrame frame) throws Exception {
        if (frame.collectionId() == null || frame.collectionId()
                                                 .isBlank()) {
            this.sendReplyMessage(session, WsResponse.error("collectionId is required to unsubscribe")); return;
        } this.subscriptionRegistryService.unSubscribe(frame.collectionId(), session);
        this.sendReplyMessage(session, WsResponse.status("unsubscribed", frame.collectionId(), null));
        log.info("Session {} unsubscribed from collection {}", session.getId(), frame.collectionId());
    }
    
    private void sendReplyMessage(WebSocketSession session, WsResponse response) throws Exception {
        String json = this.objectMapper.writeValueAsString(response); session.sendMessage(new TextMessage(json));
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        this.subscriptionRegistryService.removeSession(session);
        log.info("WebSocket Session Closed: {} with Status: {}", session.getId(), status);
        activeConnections.decrementAndGet();
    }
}
