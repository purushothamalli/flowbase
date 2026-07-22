package com.flowbase.engine.tenant.service;

import com.flowbase.engine.auth.exception.AuthenticationException;
import com.flowbase.engine.common.service.IdGenerator;
import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.dto.TenantResponse;
import com.flowbase.engine.tenant.repository.TenantRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@AllArgsConstructor
class TenantServiceImpl implements TenantService {
    private final TenantRepository tenantRepository;
    private final IdGenerator idGenerator;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public Optional<Tenant> findTenantByApiKey(String apiKey) {
        return this.tenantRepository.findByApiKey(HashUtils.sha256(apiKey));
    }
    
    @Override
    public TenantResponse createTenant(String name) {
        String id = this.idGenerator.generate();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawApiKey = "fb_live_" + Base64.getUrlEncoder()
                                              .withoutPadding()
                                              .encodeToString(randomBytes);
        Tenant tenant = new Tenant(id, name, HashUtils.sha256(rawApiKey));
        this.tenantRepository.save(tenant);
        return TenantResponse.from(tenant, rawApiKey);
    }
    
    @Override
    public TenantResponse getTenant(String tenantId) {
        Optional<Tenant> tenantExists = this.tenantRepository.findById(tenantId);
        if (tenantExists.isEmpty()) throw new AuthenticationException("Invalid tenant Id");
        return TenantResponse.from(tenantExists.get(), "");
    }
}
