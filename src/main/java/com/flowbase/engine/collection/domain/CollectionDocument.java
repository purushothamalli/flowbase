package com.flowbase.engine.collection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "COLLECTION_DOCUMENTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class CollectionDocument {
    @Id
    @Column(name = "ID")
    private String id;
    @Column(name = "COLLECTION_ID")
    private String collectionId;
    @Column(name = "DATA", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> data;
    @Column(name = "CREATED_AT")
    private Instant createdAt;
    @Column(name = "UPDATED_AT")
    private Instant updatedAt;
}
