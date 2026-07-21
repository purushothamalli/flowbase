package com.flowbase.engine.job.v1;

import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.job.dto.OutboxResponse;
import com.flowbase.engine.job.dto.PublishJobRequest;
import com.flowbase.engine.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/jobs")
class JobV1Controller {
    private final JobService jobService;
    
    @GetMapping
    public ResponseEntity<List<OutboxResponse>> listJobs() {
        return ResponseEntity.ok(this.jobService.listTenantJobs(TenantContext.get()));
    }
    
    @GetMapping("/{jobId}")
    public ResponseEntity<OutboxResponse> getJobDetails(@PathVariable String jobId) {
        return ResponseEntity.ok(this.jobService.getJobDetails(jobId, TenantContext.get()));
    }
    
    @PostMapping("/{jobId}/retry")
    public ResponseEntity<OutboxResponse> retryJob(@PathVariable String jobId) {
        return ResponseEntity.ok(this.jobService.retryJob(jobId, TenantContext.get()));
    }

    @PostMapping("/publish")
    public ResponseEntity<OutboxResponse> publishJob(@RequestBody PublishJobRequest request) {
        return ResponseEntity.status(201).body(this.jobService.publishJob(request.eventType(), request.payload(), TenantContext.get()));
    }
}
