package com.flowbase.engine.realtime.listener;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.service.AclAuthContext;
import com.flowbase.engine.collection.service.AclEvaluatorService;
import com.flowbase.engine.realtime.service.SubscriptionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisMessageBroadcastListener {
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final CollectionRepository collectionRepository;
    private final AclEvaluatorService aclEvaluatorService;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public void onMessage(String message) {
        log.info("Received realtime event from Redis Pub/Sub: {}", message);
        try {
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            String action = (String) eventData.get("action");
            String collectionId = (String) eventData.get("collectionId");
            Map<String, Object> docDto = (Map<String, Object>) eventData.get("document");

            Set<WebSocketSession> sessions = this.subscriptionRegistryService.getSessions(collectionId);
            if (sessions.isEmpty()) return;

            Optional<Collection> collectionExists = this.collectionRepository.findById(collectionId);
            if (collectionExists.isEmpty()) return;
            Collection collection = collectionExists.get();
            String readRule = collection.readRule();

            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) continue;
                try {
                    AuthenticatedUser user = (AuthenticatedUser) session.getAttributes().get("user");
                    AclAuthContext aclAuthContext = new AclAuthContext(
                        user != null ? user.id() : "anonymous",
                        user != null && user.role() != null ? user.role().name() : "anonymous"
                    );
                    
                    Map<String, Object> documentDataFields = (Map<String, Object>) docDto.get("data");
                    boolean isAllowed = this.aclEvaluatorService.evaluate(readRule, documentDataFields, aclAuthContext);
                    
                    if (isAllowed) {
                        Map<String, Object> payload = Map.of(
                            "event", action,
                            "collectionId", collectionId,
                            "data", docDto
                        );
                        String json = this.objectMapper.writeValueAsString(payload);
                        session.sendMessage(new TextMessage(json));
                        log.info("Broadcasted scaled event {} on collection {} to session {}", action, collectionId, session.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to broadcast realtime event to session: {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse or dispatch Redis Pub/Sub message", e);
        }
    }
}
