package com.flowbase.engine.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flowbase.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int defaultCapacity = 10;
    private int refillRate = 2;
}
