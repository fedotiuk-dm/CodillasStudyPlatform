package de.codillas.files.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.domain.model.StoredFile;
import de.codillas.files.domain.repository.StoredFileRepository;
import de.codillas.files.mapper.StoredFileMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private final StoredFileRepository repository;
  private final ObjectStorage storage;
  private final StoredFileMapper mapper;

  @Override
  @Transactional
  public StoredFileResponse upload(
      MultipartFile file, FileReferenceType referenceType, UUID referenceId, UUID uploaderId) {
    if (file.isEmpty()) {
      throw new BadRequestException("Uploaded file is empty");
    }
    String key = UUID.randomUUID().toString();
    String contentType =
        file.getContentType() != null ? file.getContentType() : DEFAULT_CONTENT_TYPE;
    String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : key;
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException _) {
      throw new BadRequestException("Could not read the uploaded file");
    }

    storage.put(key, bytes, contentType);
    StoredFile saved =
        repository.save(
            mapper.toEntity(
                key,
                filename,
                contentType,
                file.getSize(),
                uploaderId,
                referenceType,
                referenceId));
    return mapper.toResponse(saved);
  }

  @Override
  public FileContent download(UUID fileId) {
    StoredFile file = findByIdOrThrow(fileId);
    return new FileContent(
        new ByteArrayResource(storage.get(file.getStorageKey())),
        file.getOriginalFilename(),
        file.getContentType());
  }

  @Override
  public StoredFileResponse getMetadata(UUID fileId) {
    return mapper.toResponse(findByIdOrThrow(fileId));
  }

  @Override
  @Transactional
  public void delete(UUID fileId) {
    StoredFile file = findByIdOrThrow(fileId);
    storage.delete(file.getStorageKey());
    repository.delete(file);
  }

  private StoredFile findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("File", id));
  }
}
