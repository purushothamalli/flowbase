package com.flowbase.engine.collection.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Entity
@Table(name = "COLLECTIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class Collection {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "TENANT_ID")
    private String tenantId;
    @Column(name = "NAME")
    private String name;
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = CollectionField.class)
    private List<CollectionField> fields;
}
