package com.flowbase.engine.storage.provider;

import com.flowbase.engine.storage.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "flowbase.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageProviderService implements StorageProvider {
    @Value("${flowbase.storage.local-dir:./uploads}")
    private String baseDir;
    
    @Override
    public String store(String tenantId, String fileName, InputStream inputStream, long sizeBytes, String contentType) {
        try {
            Path tenantDir = Paths.get(baseDir, tenantId);
            Files.createDirectories(tenantDir);
            String safeFileName = fileName != null ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
            String storageKey = tenantId + "/" + UUID.randomUUID() + "_" + safeFileName;
            Path targetFile = Paths.get(baseDir).resolve(storageKey).normalize();
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (Exception e) {
            throw new StorageException("Failed to store file: " + e.getMessage());
        }
    }
    
    @Override
    public Resource load(String storageKey) {
        try {
            Path file = Paths.get(baseDir)
                             .resolve(storageKey)
                             .normalize();
            if (!Files.exists(file)) throw new StorageException("File not found!");
            return new UrlResource(file.toUri());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public boolean delete(String storageKey) {
        try {
            Path file = Paths.get(baseDir)
                             .resolve(storageKey)
                             .normalize();
            return Files.deleteIfExists(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
