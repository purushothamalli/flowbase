package com.flowbase.engine.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "TENANTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class Tenant {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "API_KEY")
    private String apiKey;
}
