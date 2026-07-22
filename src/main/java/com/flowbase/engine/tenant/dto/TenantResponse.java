package com.flowbase.engine.tenant.dto;

import com.flowbase.engine.tenant.domain.Tenant;

public record TenantResponse(String id, String name, String apiKey) {
    public static TenantResponse from(Tenant tenant, String apiKey) {
        return new TenantResponse(tenant.id(), tenant.name(), apiKey);
    }
}
