package com.flowbase.engine.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class User {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "TENANT_ID")
    private String tenantId;
    @Column(name = "EMAIL")
    private String email;
    @Column(name = "PASSWORD_HASH")
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE")
    private UserRole role;
    
    public User withPasswordHash(String newHash) {
        return new User(this.id, this.tenantId, this.email, newHash, this.role);
    }
    
    public User withRole(UserRole newRole) {
        return new User(this.id, this.tenantId, this.email, this.passwordHash, newRole);
    }
}
