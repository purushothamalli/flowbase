package com.flowbase.engine.storage.service;

import com.flowbase.engine.storage.domain.FileMetadata;
import com.flowbase.engine.storage.dto.FileMetadataResponse;
import com.flowbase.engine.storage.exception.StorageException;
import com.flowbase.engine.storage.provider.StorageProvider;
import com.flowbase.engine.storage.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageProvider storageProvider;
    
    public FileMetadataResponse uploadFile(MultipartFile file, String tenantId, String uploaderId) {
        try {
            String fileId = UUID.randomUUID()
                                .toString();
            String storageKey = this.storageProvider.store(tenantId, file.getOriginalFilename(), file.getInputStream(), file.getSize(), file.getContentType());
            FileMetadata savedMetadata = this.fileMetadataRepository.save(new FileMetadata(fileId, tenantId, uploaderId, file.getOriginalFilename(), file.getContentType(), file.getSize(), storageKey, Instant.now()));
            return FileMetadataResponse.from(savedMetadata);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public FileMetadataResponse getMetaData(String fileId, String tenantId) {
        Optional<FileMetadata> fileMetadataExists = this.fileMetadataRepository.findById(fileId);
        if (fileMetadataExists.isEmpty()) throw new StorageException("File not found!");
        
        FileMetadata fileMetadata = fileMetadataExists.get();
        if (!fileMetadata.tenantId()
                         .equals(tenantId)) throw new StorageException("File not found!");
        return FileMetadataResponse.from(fileMetadata);
    }
    
    public Resource downloadFile(String fileId, String tenantId) {
        Optional<FileMetadata> fileMetadataExists = this.fileMetadataRepository.findById(fileId);
        if (fileMetadataExists.isEmpty()) throw new StorageException("File not found!");
        
        FileMetadata fileMetadata = fileMetadataExists.get();
        if (!fileMetadata.tenantId()
                         .equals(tenantId)) throw new StorageException("File not found!");
        return this.storageProvider.load(fileMetadata.storageKey());
    }
    
    public void deleteFile(String fileId, String tenantId) {
        Optional<FileMetadata> fileMetadataExists = this.fileMetadataRepository.findById(fileId);
        if (fileMetadataExists.isEmpty()) throw new StorageException("File not found!");
        
        FileMetadata fileMetadata = fileMetadataExists.get();
        if (!fileMetadata.tenantId()
                         .equals(tenantId)) throw new StorageException("File not found!");
        this.storageProvider.delete(fileMetadata.storageKey());
        this.fileMetadataRepository.delete(fileMetadata);
    }
    
}
