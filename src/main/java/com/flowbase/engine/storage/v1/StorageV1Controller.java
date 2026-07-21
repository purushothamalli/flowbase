package com.flowbase.engine.storage.v1;

import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.config.UserContext;
import com.flowbase.engine.storage.service.StorageService;
import com.flowbase.engine.storage.dto.FileMetadataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/storage")
class StorageV1Controller {
    private final StorageService storageService;
    
    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        FileMetadataResponse response = this.storageService.uploadFile(file, TenantContext.get(), UserContext.get()
                                                                                                             .id());
        return ResponseEntity.created(ServletUriComponentsBuilder
                                     .fromCurrentRequest()
                                     .path("/{id}")
                                     .buildAndExpand(response.id())
                                     .toUri())
                             .body(response);
    }
    
    @GetMapping("/files/{fileId}")
    public ResponseEntity<FileMetadataResponse> getFileMetadata(@PathVariable String fileId) {
        return ResponseEntity.ok(this.storageService.getMetaData(fileId, TenantContext.get()));
    }
    
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        FileMetadataResponse metadataResponse = this.storageService.getMetaData(fileId, TenantContext.get());
        Resource resource = this.storageService.downloadFile(fileId, TenantContext.get());
        return ResponseEntity.ok()
                             .header(HttpHeaders.CONTENT_TYPE, metadataResponse.contentType())
                             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadataResponse.filename() + "\"")
                             .body(resource);
    }
    
    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        this.storageService.deleteFile(fileId, TenantContext.get());
        return ResponseEntity.noContent()
                             .build();
    }
}
