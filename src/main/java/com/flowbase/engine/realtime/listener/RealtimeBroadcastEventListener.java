package com.flowbase.engine.realtime.listener;

import com.flowbase.engine.realtime.event.DocumentChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RealtimeBroadcastEventListener {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocumentChangeEvent(DocumentChangeEvent event) {
        try {
            var doc = event.collectionDocument();
            var docDto = new com.flowbase.engine.collection.dto.CollectionDocumentResponse(
                doc.id(), doc.collectionId(), doc.data(), doc.createdAt(), doc.updatedAt()
            );
            Map<String, Object> redisPayload = Map.of(
                "action", event.action(),
                "collectionId", event.collectionId(),
                "document", docDto
            );
            String json = this.objectMapper.writeValueAsString(redisPayload);
            this.redisTemplate.convertAndSend("flowbase-realtime-channel", json);
            log.info("Published realtime event {} for collection {} to Redis channel", event.action(), event.collectionId());
        } catch (Exception e) {
            log.error("Failed to publish document change event to Redis", e);
        }
    }
}
