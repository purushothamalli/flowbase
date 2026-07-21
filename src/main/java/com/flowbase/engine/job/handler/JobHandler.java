package com.flowbase.engine.job.handler;

import com.flowbase.engine.job.domain.OutboxEvent;

public interface JobHandler {
    String getEventType();
    
    void handle(OutboxEvent event) throws Exception;
}
