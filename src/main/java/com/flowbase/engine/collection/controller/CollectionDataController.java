package com.flowbase.engine.collection.controller;

import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.service.CollectionDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/data/{collectionId}")
class CollectionDataController {
    private final CollectionDataService collectionDataService;
    
    @PostMapping
    public ResponseEntity<CollectionDocument> createDocument(@PathVariable String collectionId) {
        return null;
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<CollectionDocument> getDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        return null;
    }
    
    @GetMapping
    public ResponseEntity<List<CollectionDocument>> listDocuments(@PathVariable String collectionId) {
        return null;
    }
    
    @PatchMapping("/{documentId}")
    public ResponseEntity<CollectionDocument> updateDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        return null;
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        return null;
    }
}
