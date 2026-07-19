package com.flowbase.engine.tenant.dto;

public record TenantResponse(String id, String name, String apiKey) {
    public TenantResponse omitApiKey() {
        return new TenantResponse(id, name, "");
    }
}
