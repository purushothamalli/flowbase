package com.flowbase.engine.collection.controller;

import com.flowbase.engine.collection.domain.CollectionDocument;
import com.flowbase.engine.collection.dto.CollectionDocumentResponse;
import com.flowbase.engine.collection.service.CollectionDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/data/{collectionId}")
class CollectionDataController {
    private final CollectionDataService collectionDataService;
    
    @PostMapping
    public ResponseEntity<CollectionDocumentResponse> createDocument(@PathVariable String collectionId, @RequestBody Map<String, Object> payload) {
        CollectionDocument document = this.collectionDataService.insertDocument(collectionId, payload);
        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(document.id())
                        .toUri()
                ).body(this.mapToResponse(document));
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<CollectionDocumentResponse> getDocument(@PathVariable String collectionId, @PathVariable String documentId) {
        CollectionDocument document = this.collectionDataService.getDocument(collectionId, documentId);
        return ResponseEntity.ok(this.mapToResponse(document));
    }
    
    @GetMapping
    public ResponseEntity<List<CollectionDocumentResponse>> listDocuments(@PathVariable String collectionId) {
        List<CollectionDocument> collectionDocuments = this.collectionDataService.findDocumentsByCollection(collectionId);
        return ResponseEntity.ok(collectionDocuments.stream().map(this::mapToResponse).toList());
    }
    
    @PatchMapping("/{documentId}")
    public ResponseEntity<CollectionDocumentResponse> updateDocument(@PathVariable String collectionId, @PathVariable String documentId, @RequestBody Map<String, Object> payload) {
        CollectionDocument updatedDocument = this.collectionDataService.updateDocument(collectionId, documentId, payload);
        return ResponseEntity.ok(this.mapToResponse(updatedDocument));
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String collectionId, @PathVariable String documentId, @RequestBody Map<String, Object> payload) {
        this.collectionDataService.deleteDocument(collectionId, documentId); return ResponseEntity.noContent().build();
    }
    
    private CollectionDocumentResponse mapToResponse(CollectionDocument document) {
        return new CollectionDocumentResponse(document.id(), document.collectionId(), document.data(), document.createdAt(), document.updatedAt());
    }
}
