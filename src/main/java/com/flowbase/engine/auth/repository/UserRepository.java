package com.flowbase.engine.auth.repository;

import com.flowbase.engine.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByTenantIdAndEmail(String tenantId, String email);
    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
