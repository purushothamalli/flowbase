package com.flowbase.engine.realtime.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SubscriptionRegistryService {
    private final Map<String, Set<WebSocketSession>> registry = new ConcurrentHashMap<>();
    
    public void subscribe(String collectionId, WebSocketSession session) {
        this.registry.computeIfAbsent(collectionId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }
    
    public void unSubscribe(String collectionId, WebSocketSession session) {
        Set<WebSocketSession> sessions = this.registry.get(collectionId);
        if (sessions != null) sessions.remove(session);
    }
    
    public void removeSession(WebSocketSession session) {
        this.registry.values().forEach(sessions -> sessions.remove(session));
    }
    
    public Set<WebSocketSession> getSessions(String collectionId) {
        return this.registry.getOrDefault(collectionId, Collections.emptySet());
    }
}
