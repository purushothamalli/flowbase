package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheLockManager {
    private final CacheService cacheService;

    public void evictCollectionCache(String collectionId) {
        this.cacheService.evictNamespace("flowbase:cache:" + collectionId + ":");
    }

    public List<CollectionDocument> getCachedList(String cacheKey) {
        return this.cacheService.getList(cacheKey, CollectionDocument.class);
    }

    public String generateCacheKey(String collectionId, Object... inputs) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Object input : inputs) {
                sb.append(input != null ? input.toString() : "null").append("|");
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String hexString = Integer.toHexString(0xff & b);
                if (hexString.length() == 1) hex.append('0');
                hex.append(hexString);
            }
            return "flowbase:cache:" + collectionId + ":" + hex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CacheKey :", e);
        }
    }

    public List<CollectionDocument> getDocumentsWithStampedeProtection(
            String cacheKey, 
            boolean bypassCache, 
            Collection collection, 
            Supplier<List<CollectionDocument>> dbQuerySupplier) {
        if (bypassCache) return dbQuerySupplier.get();
        List<CollectionDocument> cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
        if (cached != null) return cached;
        String lockKey = cacheKey + ":lock";
        String lockValue = UUID.randomUUID().toString();
        int lockTtl = collection.lockTtlSeconds() != null ? collection.lockTtlSeconds().intValue() : 5;
        int cacheTtl = collection.cacheTtlSeconds() != null ? collection.cacheTtlSeconds().intValue() : 3600;
        if (this.cacheService.acquireLock(lockKey, lockValue, lockTtl)) {
            try {
                cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
                if (cached != null) return cached;
                List<CollectionDocument> dbResult = dbQuerySupplier.get();
                this.cacheService.put(cacheKey, dbResult, cacheTtl);
                return dbResult;
            } finally {
                this.cacheService.releaseLock(lockKey, lockValue);
            }
        } else {
            int retries = 10;
            while (retries > 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                cached = this.cacheService.getList(cacheKey, CollectionDocument.class);
                if (cached != null) return cached;
                retries--;
            }
            return dbQuerySupplier.get();
        }
    }
}
