package com.flowbase.engine.config;

import com.flowbase.engine.tenant.domain.Tenant;
import com.flowbase.engine.tenant.repository.TenantRepository;
import com.flowbase.engine.tenant.service.HashUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
@AllArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {
    private final TenantRepository tenantRepository;
    
    @Override
    public void run(String @NonNull ... args) throws Exception {
        try {
            String expectedHash = HashUtils.sha256("fb_test_key_abc123");
            java.util.Optional<Tenant> existingTenant = tenantRepository.findById("default-tenant");
            
            if (existingTenant.isPresent()) {
                Tenant tenant = existingTenant.get();
                // If the key in the database is not the correct hash, update it:
                if (!expectedHash.equals(tenant.apiKey())) {
                    tenant.apiKey(expectedHash);
                    tenantRepository.save(tenant);
                    log.info("Outdated API key detected. Updated default-tenant to hashed key.");
                }
            } else {
                Tenant defaultTenant = new Tenant("default-tenant", "Default Sandbox", expectedHash);
                tenantRepository.save(defaultTenant);
                log.info("Seeded default-tenant successfully.");
            }
        } catch (Exception e) {
            log.error("Database seeding failed: ", e);
        }
    }
}
