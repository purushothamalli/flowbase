package com.flowbase.engine.collection.domain;

import java.util.Optional;

public interface CollectionRepository {
    Optional<Collection> findById(String id);
    Collection save(Collection collection);
}
