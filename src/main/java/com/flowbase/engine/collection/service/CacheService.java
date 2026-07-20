package com.flowbase.engine.collection.service;

import java.util.List;

public interface CacheService {
    void put(String key, Object value, long ttlSeconds);
    
    <T> T get(String key, Class<T> type);
    
    <T> List<T> getList(String key, Class<T> elementType);
    
    void evictNamespace(String prefix);
    
    boolean acquireLock(String lockKey, String lockValue, long ttlSeconds);
    
    void releaseLock(String lockKey, String lockValue);
}
