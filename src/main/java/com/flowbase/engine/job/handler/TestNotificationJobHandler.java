package com.flowbase.engine.job.handler;

import com.flowbase.engine.job.domain.OutboxEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestNotificationJobHandler implements JobHandler {
    @Override
    public String getEventType() {
        return "NOTIFICATION_SEND";
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        log.info("▶ Processing NOTIFICATION_SEND job [{}]: payload={}", event.id(), event.payload());
        if (event.payload() != null && event.payload().contains("SIMULATE_FAILURE")) {
            throw new RuntimeException("Simulated job execution failure");
        }
    }
}
