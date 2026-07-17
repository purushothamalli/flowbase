package com.flowbase.engine.collection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "COLLECTION_FIELDS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class CollectionField {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "NAME")
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE")
    private FieldType type;
    @Column(name = "REQUIRED")
    private boolean required;
    @ManyToOne
    @JoinColumn(name = "COLLECTION_ID", nullable = false, referencedColumnName = "ID")
    private Collection collection;
}
