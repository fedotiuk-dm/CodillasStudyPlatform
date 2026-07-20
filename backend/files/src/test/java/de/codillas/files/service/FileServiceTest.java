package de.codillas.files.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.domain.model.StoredFile;
import de.codillas.files.domain.repository.StoredFileRepository;
import de.codillas.files.mapper.StoredFileMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService")
class FileServiceTest {

  @Mock private StoredFileRepository repository;
  @Mock private ObjectStorage storage;
  @Mock private StoredFileMapper mapper;
  @Mock private CurrentUser currentUser;
  @Mock private ApplicationEventPublisher events;
  private FileServiceImpl service;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    service =
        new FileServiceImpl(
            repository,
            storage,
            mapper,
            currentUser,
            java.util.List.of(),
            new de.codillas.files.config.FilesProperties("b", "e", "r", "a", "s", null),
            events);
  }

  @Test
  @DisplayName("upload stores the bytes and persists the metadata")
  void upload_storesAndPersists() throws Exception {
    UUID uploader = UUID.randomUUID();
    byte[] bytes = "hello".getBytes();
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getContentType()).thenReturn("text/plain");
    when(file.getOriginalFilename()).thenReturn("a.txt");
    when(file.getSize()).thenReturn(5L);
    when(file.getBytes()).thenReturn(bytes);
    StoredFile saved = StoredFile.builder().storageKey("k").build();
    StoredFileResponse dto = mock(StoredFileResponse.class);
    when(mapper.toEntity(
            anyString(), eq("a.txt"), eq("text/plain"), anyLong(), eq(uploader), any(), any()))
        .thenReturn(saved);
    when(repository.save(saved)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.upload(file, null, null, uploader)).isSameAs(dto);
    verify(storage).put(anyString(), eq(bytes), eq("text/plain"));
  }

  @Test
  @DisplayName("upload with an allow-list accepts a permitted content type within size")
  void upload_allowedContentType_passes() throws Exception {
    FileServiceImpl restricted =
        new FileServiceImpl(
            repository,
            storage,
            mapper,
            currentUser,
            java.util.List.of(),
            new de.codillas.files.config.FilesProperties(
                "b", "e", "r", "a", "s", java.util.List.of("text/plain")),
            events);

    UUID uploader = UUID.randomUUID();
    byte[] bytes = "hello".getBytes();
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getContentType()).thenReturn("text/plain");
    when(file.getOriginalFilename()).thenReturn("a.txt");
    when(file.getSize()).thenReturn(5L);
    when(file.getBytes()).thenReturn(bytes);
    StoredFile saved = StoredFile.builder().storageKey("k").build();
    StoredFileResponse dto = mock(StoredFileResponse.class);
    when(mapper.toEntity(
            anyString(), eq("a.txt"), eq("text/plain"), anyLong(), eq(uploader), any(), any()))
        .thenReturn(saved);
    when(repository.save(saved)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(restricted.upload(file, null, null, uploader)).isSameAs(dto);
    verify(storage).put(anyString(), eq(bytes), eq("text/plain"));
  }

  @Test
  @DisplayName("upload with an allow-list rejects a disallowed content type")
  void upload_disallowedContentType_rejected() {
    FileServiceImpl restricted =
        new FileServiceImpl(
            repository,
            storage,
            mapper,
            currentUser,
            java.util.List.of(),
            new de.codillas.files.config.FilesProperties(
                "b", "e", "r", "a", "s", java.util.List.of("text/plain")),
            events);

    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getContentType()).thenReturn("application/x-msdownload");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> restricted.upload(file, null, null, UUID.randomUUID()));
  }

  @Test
  @DisplayName("upload rejects an empty file")
  void upload_empty_rejected() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(true);
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.upload(file, null, null, UUID.randomUUID()));
  }

  @Test
  @DisplayName("download returns the bytes from storage with the file's headers")
  void download_returnsContent() throws Exception {
    UUID fileId = UUID.randomUUID();
    UUID uploader = UUID.randomUUID();
    StoredFile file =
        StoredFile.builder()
            .storageKey("k")
            .originalFilename("a.txt")
            .contentType("text/plain")
            .uploadedBy(uploader)
            .build();
    when(repository.findById(fileId)).thenReturn(Optional.of(file));
    when(currentUser.id()).thenReturn(uploader);
    when(storage.get("k")).thenReturn("hi".getBytes());

    FileContent content = service.download(fileId);

    assertThat(content.filename()).isEqualTo("a.txt");
    assertThat(content.contentType()).isEqualTo("text/plain");
    assertThat(content.resource().getContentAsByteArray()).isEqualTo("hi".getBytes());
  }

  @Test
  @DisplayName("download of a loose file by a non-owner non-staff caller is 404")
  void download_loose_nonOwner_notFound() {
    UUID fileId = UUID.randomUUID();
    StoredFile file = StoredFile.builder().storageKey("k").uploadedBy(UUID.randomUUID()).build();
    when(repository.findById(fileId)).thenReturn(Optional.of(file));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(currentUser.isStaff()).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.download(fileId));
  }

  @Test
  @DisplayName("listMyFiles returns the caller's uploads, newest first by default")
  void listMyFiles_returnsCallersUploads() {
    UUID uploader = UUID.randomUUID();
    org.springframework.data.domain.Page<StoredFile> page =
        new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(StoredFile.builder().storageKey("k").uploadedBy(uploader).build()));
    de.codillas.files.api.dto.FileListResponse dto =
        mock(de.codillas.files.api.dto.FileListResponse.class);
    when(repository.findByUploadedBy(
            eq(uploader), any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(dto);

    assertThat(service.listMyFiles(uploader, org.springframework.data.domain.PageRequest.of(0, 20)))
        .isSameAs(dto);
  }

  @Test
  @DisplayName("delete removes the object then the metadata")
  void delete_removesBoth() {
    UUID fileId = UUID.randomUUID();
    StoredFile file = StoredFile.builder().storageKey("k").build();
    when(repository.findById(fileId)).thenReturn(Optional.of(file));

    service.delete(fileId);

    verify(storage).delete("k");
    verify(repository).delete(file);
  }

  @Test
  @DisplayName("metadata for a missing file is a 404")
  void metadata_missing_notFound() {
    UUID fileId = UUID.randomUUID();
    when(repository.findById(fileId)).thenReturn(Optional.empty());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getMetadata(fileId));
  }

  @Test
  @DisplayName("delete announces FileDeleted so course can drop the orphaned materials")
  void delete_publishesFileDeleted() {
    UUID fileId = UUID.randomUUID();
    StoredFile file = StoredFile.builder().storageKey("k").build();
    when(repository.findById(fileId)).thenReturn(java.util.Optional.of(file));

    service.delete(fileId);

    verify(storage).delete("k");
    verify(repository).delete(file);
    verify(events).publishEvent(new de.codillas.shared.event.FileDeleted(fileId));
  }
}
