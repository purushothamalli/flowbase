package com.flowbase.engine.job.service;

import com.flowbase.engine.job.domain.OutboxEvent;
import com.flowbase.engine.job.domain.OutboxStatus;
import com.flowbase.engine.job.dto.OutboxResponse;
import com.flowbase.engine.job.exception.JobException;
import com.flowbase.engine.job.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisherService outboxEventPublisherService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    
    public OutboxResponse publishJob(String eventType, Object payload, String tenantId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = "idempotency:job:" + tenantId + ":" + idempotencyKey;
            String tempId = java.util.UUID.randomUUID().toString();
            
            Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, tempId, java.time.Duration.ofHours(24));
            if (Boolean.FALSE.equals(success)) {
                // Key already exists. Retrieve stored outbox event response details
                String existingJobId = redisTemplate.opsForValue().get(redisKey);
                return this.getJobDetails(existingJobId, tenantId);
            }
            
            OutboxEvent event = this.outboxEventPublisherService.publish(tenantId, eventType, payload);
            redisTemplate.opsForValue().set(redisKey, event.id(), java.time.Duration.ofHours(24));
            return OutboxResponse.from(event);
        }
        
        OutboxEvent event = this.outboxEventPublisherService.publish(tenantId, eventType, payload);
        return OutboxResponse.from(event);
    }
    
    public List<OutboxResponse> listTenantJobs(String tenantId) {
        return this.outboxEventRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                                         .stream()
                                         .map(OutboxResponse::from)
                                         .toList();
    }
    
    public OutboxResponse getJobDetails(String jobId, String tenantId) {
        OutboxEvent event =
                this.outboxEventRepository.findById(jobId)
                                          .orElseThrow(() -> new JobException("Job not found: " + jobId));
        if (!event.tenantId()
                  .equals(tenantId)) throw new JobException("Job not found: " + jobId);
        return OutboxResponse.from(event);
    }
    
    @Transactional
    public OutboxResponse retryJob(String jobId, String tenantId) {
        OutboxEvent event =
                this.outboxEventRepository.findById(jobId)
                                          .orElseThrow(() -> new JobException("Job not found: " + jobId));
        if (!event.tenantId()
                  .equals(tenantId)) throw new JobException("Job not found: " + jobId);
        event.status(OutboxStatus.PENDING);
        event.retryCount(0);
        event.errorMessage(null);
        event.updatedAt(Instant.now());
        return OutboxResponse.from(this.outboxEventRepository.save(event));
    }
}
