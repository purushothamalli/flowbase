package com.flowbase.engine.tenant.service;

import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.dto.TenantResponse;
import com.flowbase.engine.tenant.repository.TenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;
    private final IdGenerator idGenerator;
    
    @Override
    public Optional<Tenant> findTenantByApiKey(String apiKey) {
        return this.tenantRepository.findByApiKey(HashUtils.sha256(apiKey));
    }
    
    @Override
    public TenantResponse createTenant(String name) {
        String id = this.idGenerator.generate();
        String rawApiKey = "fb_live_" + this.idGenerator.generate().replace("-", "");
        Tenant tenant = new Tenant(id, name, HashUtils.sha256(rawApiKey));
        this.tenantRepository.save(tenant);
        return new TenantResponse(id, name, rawApiKey);
    }
    
    @Override
    public TenantResponse getTenant(String tenantId) {
        Optional<Tenant> tenantExists = this.tenantRepository.findById(tenantId);
        if (tenantExists.isEmpty()) throw new AuthenticationException("Invalid tenant Id");
        return new TenantResponse(tenantExists.get().id(), tenantExists.get().name(), tenantExists.get().apiKey());
    }
}
