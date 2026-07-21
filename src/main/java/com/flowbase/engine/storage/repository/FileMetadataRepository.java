package com.flowbase.engine.storage.repository;

import com.flowbase.engine.storage.domain.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {}
