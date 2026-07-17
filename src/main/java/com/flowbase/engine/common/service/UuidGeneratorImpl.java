package com.flowbase.engine.common.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class UuidGeneratorImpl implements IdGenerator {
    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
