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
    
    public OutboxResponse publishJob(String eventType, Object payload, String tenantId) {
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
