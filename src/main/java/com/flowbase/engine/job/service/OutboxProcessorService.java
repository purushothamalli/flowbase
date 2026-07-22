package com.flowbase.engine.job.service;

import com.flowbase.engine.job.domain.OutboxEvent;
import com.flowbase.engine.job.domain.OutboxStatus;
import com.flowbase.engine.job.domain.JobDlq;
import com.flowbase.engine.job.repository.JobDlqRepository;
import com.flowbase.engine.job.handler.JobHandler;
import com.flowbase.engine.job.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class OutboxProcessorService {
    private final OutboxEventRepository outboxEventRepository;
    private final JobDlqRepository jobDlqRepository;
    private final Map<String, JobHandler> handlers = new HashMap<>();
    private final List<JobHandler> handlerList;
    private final MeterRegistry meterRegistry;
    
    @PostConstruct
    private void init() {
        for (JobHandler handler : this.handlerList) {
            this.handlers.put(handler.getEventType(), handler);
            log.info("Registerd JobHandler for event type {}", handler.getEventType());
        }
    }
    
    @Scheduled(fixedDelay = 2000)
    public void processOutBox() {
        List<OutboxEvent> leasedJobs = this.outboxEventRepository.leasePendingJobs(10);
        if (leasedJobs.isEmpty()) return;
        for (OutboxEvent event : leasedJobs) this.processSingleEvent(event);
    }
    
    @Transactional
    public void processSingleEvent(OutboxEvent event) {
        event.status(OutboxStatus.PROCESSING);
        event.leasedUntil(Instant.now()
                                 .plusSeconds(60));
        event.updatedAt(Instant.now());
        this.outboxEventRepository.save(event);
        JobHandler handler = this.handlers.get(event.eventType());
        if (handler == null) {
            log.warn("No JobHandler registered for event type: {}", event.eventType());
            event.status(OutboxStatus.FAILED);
            event.errorMessage("No handler registerd for event type: " + event.eventType());
            event.updatedAt(Instant.now());
            this.outboxEventRepository.save(event);
            return;
        }
        
        io.micrometer.core.instrument.Timer.Sample sample = io.micrometer.core.instrument.Timer.start(meterRegistry);
        try {
            handler.handle(event);
            event.status(OutboxStatus.COMPLETED);
            event.errorMessage(null);
            event.updatedAt(Instant.now());
            sample.stop(io.micrometer.core.instrument.Timer.builder("flowbase_jobs_latency")
                    .tag("eventType", event.eventType())
                    .tag("status", "success")
                    .register(meterRegistry));
            this.meterRegistry.counter("flowbase_jobs_processed_total", "status", "success")
                              .increment();
            log.info("Outbox job [{}] completed successfully.", event.id());
        } catch (Exception e) {
            log.error("Outbox job [{}] execution failed {}", event.id(), e.getMessage());
            sample.stop(io.micrometer.core.instrument.Timer.builder("flowbase_jobs_latency")
                    .tag("eventType", event.eventType())
                    .tag("status", "failed")
                    .register(meterRegistry));
            this.meterRegistry.counter("flowbase_jobs_processed_total", "status", "failed")
                              .increment();
            event.retryCount(event.retryCount() + 1);
            event.errorMessage(e.getMessage());
            
            if (event.retryCount() >= event.maxRetries()) {
                event.status(OutboxStatus.FAILED);
                
                // Move to Dead Letter Queue (DLQ)
                JobDlq dlq = new JobDlq(
                    java.util.UUID.randomUUID().toString(),
                    event.eventType(),
                    event.tenantId(),
                    event.payload(),
                    e.getMessage(),
                    Instant.now()
                );
                this.jobDlqRepository.save(dlq);
                
                this.meterRegistry.counter("flowbase_jobs_dlq_total", "eventType", event.eventType()).increment();
                log.error("🚨 Poison Job [{}] exceeded max retries. Persistent DLQ created.", event.id());
            } else {
                event.status(OutboxStatus.PENDING);
            }
        }
        this.outboxEventRepository.save(event);
    }
}
