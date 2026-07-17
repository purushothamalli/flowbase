package com.flowbase.engine.auth.domain;

public record User(String id, String email, String passwordHash, UserRole role) {
    public User withPasswordHash(String newHash) {
        return new User(this.id, this.email, newHash, this.role);
    }
    
    public User withRole(UserRole newRole) {
        return new User(this.id, this.email, this.passwordHash, newRole);
    }
}
