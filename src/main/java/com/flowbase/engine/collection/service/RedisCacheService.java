package com.flowbase.engine.collection.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
class RedisCacheService implements CacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void put(String key, Object value, long ttlSeconds) {
        String val = this.objectMapper.writeValueAsString(value);
        try {
            this.redisTemplate.opsForValue().set(key, val, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error(e.toString());
        }
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
        String val = this.redisTemplate.opsForValue().get(key);
        if (val != null) return this.objectMapper.readValue(val, type);
        
        return null;
    }
    
    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        String val = this.redisTemplate.opsForValue().get(key); if (val != null)
            return this.objectMapper.readValue(val, this.objectMapper.getTypeFactory()
                                                                     .constructCollectionType(List.class, elementType));
        return null;
    }
    
    @Override
    public void evictNamespace(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build(); try {
            this.redisTemplate.execute((RedisConnection connection) -> {
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    List<String> keysToDelete = new ArrayList<>();
                    while (cursor.hasNext()) keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    if (!keysToDelete.isEmpty()) this.redisTemplate.delete(keysToDelete);
                } catch (Exception e) {
                    log.error("Failed to execute namespace eviction scan: ", e);
                } return null;
            });
        } catch (Exception e) {
            log.error("Failed to run scan execution: ", e);
        }
    }
    
    @Override
    public boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        try {
            Boolean success = this.redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue,
                    Duration.ofSeconds(ttlSeconds));
            return success != null && success;
        } catch (Exception e) {
            log.error("Failed to acquire Lock: ", e);
            return false;
        }
    }
    
    @Override
    public void releaseLock(String lockKey, String lockValue) {
        try {
            String value = this.redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(value)) this.redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.error("Failed to release Lock: ", e);
        }
    }
}
