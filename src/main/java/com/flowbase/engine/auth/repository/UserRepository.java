package com.flowbase.engine.auth.repository;

import com.flowbase.engine.auth.domain.User;

import java.util.Optional;

public interface UserRepository {
    void save(User user);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
