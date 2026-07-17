package com.flowbase.engine.tenant.service;

import com.flowbase.engine.tenant.domain.Tenant;

import java.util.Optional;

public interface TenantService {
    Optional<Tenant> findTenantByApiKey(String apiKey);
    Tenant createTenant(String name);
}
