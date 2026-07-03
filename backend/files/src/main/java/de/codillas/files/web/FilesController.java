package de.codillas.files.web;

import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.FilesApi;
import de.codillas.files.api.dto.FileListResponse;
import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.service.FileContent;
import de.codillas.files.service.FileService;
import de.codillas.shared.security.CurrentUser;
import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.shared.security.RequiresTeacher;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link FilesApi}. */
@RestController
@RequiredArgsConstructor
public class FilesController implements FilesApi {

  private final FileService fileService;
  private final CurrentUser currentUser;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<StoredFileResponse> uploadFile(
      MultipartFile file, FileReferenceType referenceType, UUID referenceId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(fileService.upload(file, referenceType, referenceId, currentUser.id()));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<FileListResponse> listMyFiles(Pageable pageable) {
    return ResponseEntity.ok(fileService.listMyFiles(currentUser.id(), pageable));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<Resource> downloadFile(UUID fileId) {
    FileContent content = fileService.download(fileId);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(content.filename()).build().toString())
        .contentType(MediaType.parseMediaType(content.contentType()))
        .body(content.resource());
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<StoredFileResponse> getFileMetadata(UUID fileId) {
    return ResponseEntity.ok(fileService.getMetadata(fileId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<Void> deleteFile(UUID fileId) {
    fileService.delete(fileId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
