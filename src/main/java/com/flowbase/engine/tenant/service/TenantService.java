package com.flowbase.engine.tenant.service;

import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.dto.TenantResponse;

import java.util.Optional;

public interface TenantService {
    Optional<Tenant> findTenantByApiKey(String apiKey);
    
    TenantResponse createTenant(String name);
    
    public TenantResponse getTenant(String tenantId);
}
