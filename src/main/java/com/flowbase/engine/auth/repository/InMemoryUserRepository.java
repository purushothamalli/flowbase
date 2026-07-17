package com.flowbase.engine.auth.repository;

import com.flowbase.engine.auth.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    @Override
    public void save(User user) {
        this.users.put(user.email(), user);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(this.users.get(email));
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return this.users.containsKey(email);
    }
}
