package com.flowbase.engine.config;

import com.flowbase.engine.auth.domain.Tenant;
import com.flowbase.engine.auth.repository.TenantRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {
    private final TenantRepository tenantRepository;
    
    @Override
    public void run(String @NonNull ... args) throws Exception {
        try {
            if (!tenantRepository.existsById("default-tenant")) {
                Tenant defaultTenant = new Tenant("default-tenant", "Default Sandbox", "fb_test_key_abc123");
                tenantRepository.save(defaultTenant);
                log.info("Tenant Seeded successfully: {}", defaultTenant);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
