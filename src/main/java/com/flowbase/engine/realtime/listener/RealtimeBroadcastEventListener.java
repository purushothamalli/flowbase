package com.flowbase.engine.realtime.listener;

import com.flowbase.engine.auth.dto.AuthenticatedUser;
import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionRepository;
import com.flowbase.engine.collection.service.AclAuthContext;
import com.flowbase.engine.collection.service.AclEvaluatorService;
import com.flowbase.engine.realtime.event.DocumentChangeEvent;
import com.flowbase.engine.realtime.service.SubscriptionRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealtimeBroadcastEventListener {
    private final SubscriptionRegistryService subscriptionRegistryService;
    private final CollectionRepository collectionRepository;
    private final AclEvaluatorService aclEvaluatorService;
    private final ObjectMapper objectMapper;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentChangeEvent(DocumentChangeEvent event) {
        Set<WebSocketSession> sessions = this.subscriptionRegistryService.getSessions(event.collectionId());
        if (sessions.isEmpty()) return;
        Optional<Collection> collectionExists = this.collectionRepository.findById(event.collectionId());
        if (collectionExists.isEmpty()) return; Collection collection = collectionExists.get();
        String readRule = collection.readRule(); for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue; try {
                AuthenticatedUser user = (AuthenticatedUser) session.getAttributes().get("user");
                AclAuthContext aclAuthContext = new AclAuthContext(user != null ? user.id() : "anonymous", user != null && user.role() != null ? user.role()
                                                                                                                                                     .name() : "anonymous");
                boolean isAllowed = this.aclEvaluatorService.evaluate(readRule, event.collectionDocument()
                                                                                     .data(), aclAuthContext);
                if (isAllowed) {
                    var doc = event.collectionDocument();
                    var docDto = new com.flowbase.engine.collection.dto.CollectionDocumentResponse(
                        doc.id(), doc.collectionId(), doc.data(), doc.createdAt(), doc.updatedAt()
                    );
                    Map<String, Object> payload = Map.of("event", event.action(), "collectionId", collection.id(), "data", docDto);
                    String json = this.objectMapper.writeValueAsString(payload);
                    session.sendMessage(new TextMessage(json));
                    log.info("Broadcasted {} event on collection {} to session {}", event.action(), collection.id(), session.getId());
                }
            } catch (Exception e) {
                log.error("Failed to broadcast realtime event to session: {}", session.getId(), e);
            }
        }
    }
}
