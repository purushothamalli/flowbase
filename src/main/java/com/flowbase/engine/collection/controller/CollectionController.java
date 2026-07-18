package com.flowbase.engine.collection.controller;

import com.flowbase.engine.collection.dto.CollectionResponse;
import com.flowbase.engine.collection.dto.CreateCollectionRequest;
import com.flowbase.engine.collection.service.CollectionService;
import com.flowbase.engine.config.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriBuilder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/collections")
class CollectionController {
    private final CollectionService collectionService;
    
    @PostMapping
    ResponseEntity<CollectionResponse> createCollection(@RequestBody CreateCollectionRequest request) {
        CollectionResponse response = this.collectionService.createCollection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    ResponseEntity<List<CollectionResponse>> getCollections() {
        List<CollectionResponse> response = this.collectionService.listCollections(TenantContext.get());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    ResponseEntity<CollectionResponse> getCollection(@PathVariable String id) {
        CollectionResponse response = this.collectionService.getCollection(id);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCollection(@PathVariable String id) {
        this.collectionService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }
}
