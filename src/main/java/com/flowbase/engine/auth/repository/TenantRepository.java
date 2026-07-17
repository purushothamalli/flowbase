package com.flowbase.engine.auth.repository;

import com.flowbase.engine.auth.domain.Tenant;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findByApiKey(String apiKey);
    
    boolean existsById(@NonNull String id);
}
