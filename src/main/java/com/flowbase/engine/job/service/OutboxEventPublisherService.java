package com.flowbase.engine.job.service;

import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.job.domain.OutboxEvent;
import com.flowbase.engine.job.domain.OutboxStatus;
import com.flowbase.engine.job.exception.JobException;
import com.flowbase.engine.job.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventPublisherService {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public OutboxEvent publish(String eventType, Object payload) {
        String tenantId = TenantContext.get();
        return this.publish(tenantId, eventType, payload);
    }
    
    @Transactional
    public OutboxEvent publish(String tenantId, String eventType, Object payload) {
        try {
            String payloadJson = payload instanceof String ? (String) payload : this.objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = new OutboxEvent(
                    UUID.randomUUID()
                        .toString(),
                    tenantId,
                    eventType,
                    payloadJson,
                    OutboxStatus.PENDING,
                    0,
                    3,
                    null,
                    null,
                    Instant.now(),
                    Instant.now());
            return this.outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            throw new JobException("Failed to publish outbox event: " + e.getMessage());
        }
    }
}
