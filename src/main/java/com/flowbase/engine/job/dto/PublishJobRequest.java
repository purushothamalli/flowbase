package com.flowbase.engine.job.dto;

public record PublishJobRequest(String eventType, Object payload) {}
