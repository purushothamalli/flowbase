package com.flowbase.engine.storage.provider;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface StorageProvider {
    String store(String tenantId, String fileName, InputStream inputStream, long sizeBytes, String contentType);
    
    Resource load(String storageKey);
    
    boolean delete(String storageKey);
}
