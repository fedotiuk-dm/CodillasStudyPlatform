package de.codillas.files.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.dto.FileListResponse;
import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.api.dto.StoredFileResponse;

public interface FileService {

  StoredFileResponse upload(
      MultipartFile file, FileReferenceType referenceType, UUID referenceId, UUID uploaderId);

  FileListResponse listMyFiles(UUID uploaderId, Pageable pageable);

  FileContent download(UUID fileId);

  StoredFileResponse getMetadata(UUID fileId);

  void delete(UUID fileId);
}
