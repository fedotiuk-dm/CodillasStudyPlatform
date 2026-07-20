package de.codillas.files.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.dto.FileListResponse;
import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.config.FilesProperties;
import de.codillas.files.domain.model.StoredFile;
import de.codillas.files.domain.repository.StoredFileRepository;
import de.codillas.files.mapper.StoredFileMapper;
import de.codillas.shared.domain.repository.GenericSpecification;
import de.codillas.shared.event.FileDeleted;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileServiceImpl implements FileService {

  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

  private final StoredFileRepository repository;
  private final ObjectStorage storage;
  private final StoredFileMapper mapper;
  private final CurrentUser currentUser;
  private final List<FileAccessAuthorizer> authorizers;
  private final FilesProperties properties;
  private final ApplicationEventPublisher events;

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
    List<String> allowed = properties.allowedContentTypes();
    if (allowed != null && !allowed.isEmpty() && !allowed.contains(contentType)) {
      throw new BadRequestException("Unsupported file type: " + contentType);
    }
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
  public FileListResponse listMyFiles(UUID uploaderId, Pageable pageable) {
    return mapper.toListResponse(
        repository.findByUploadedBy(
            uploaderId,
            GenericSpecification.withDefaultSort(pageable, StoredFileRepository.NEWEST_FIRST)));
  }

  @Override
  public FileContent download(UUID fileId) {
    StoredFile file = findByIdOrThrow(fileId);
    if (!canAccess(file)) {
      throw new NotFoundException("File", fileId); // 404, never leak existence
    }
    return new FileContent(
        new ByteArrayResource(storage.get(file.getStorageKey())),
        file.getOriginalFilename(),
        file.getContentType());
  }

  /**
   * Object-level read check, delegated to the owning module via the {@link FileAccessAuthorizer}
   * resolved by the file's reference type. A file with no matching authorizer (loose / untyped
   * upload) is readable only by its uploader or staff.
   */
  private boolean canAccess(StoredFile file) {
    UUID caller = currentUser.id();
    return authorizers.stream()
        .filter(a -> a.referenceType() == file.getReferenceType())
        .findFirst()
        .map(a -> a.canAccess(file.getReferenceId(), file.getUploadedBy(), caller))
        .orElseGet(() -> caller.equals(file.getUploadedBy()) || currentUser.isStaff());
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
    events.publishEvent(new FileDeleted(fileId));
  }

  private StoredFile findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("File", id));
  }
}
