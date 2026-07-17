package com.flowbase.engine.tenant.service;

import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.repository.TenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;
    @Override
    public Optional<Tenant> findTenantByApiKey(String apiKey) {
        return this.tenantRepository.findByApiKey(HashUtils.sha256(apiKey));
    }
    
    @Override
    public Tenant createTenant(String name) {
        String id = UUID.randomUUID().toString();
        String rawApiKey = "fb_live_" + UUID.randomUUID().toString().replace("-", "");
        Tenant tenant = new Tenant(id, name, HashUtils.sha256(rawApiKey));
        this.tenantRepository.save(tenant);
        return tenant;
    }
}
