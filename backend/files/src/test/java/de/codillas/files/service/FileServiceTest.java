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

import org.springframework.web.multipart.MultipartFile;

import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.domain.model.StoredFile;
import de.codillas.files.domain.repository.StoredFileRepository;
import de.codillas.files.mapper.StoredFileMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService")
class FileServiceTest {

  @Mock private StoredFileRepository repository;
  @Mock private ObjectStorage storage;
  @Mock private StoredFileMapper mapper;
  @InjectMocks private FileServiceImpl service;

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
    StoredFile file =
        StoredFile.builder()
            .storageKey("k")
            .originalFilename("a.txt")
            .contentType("text/plain")
            .build();
    when(repository.findById(fileId)).thenReturn(Optional.of(file));
    when(storage.get("k")).thenReturn("hi".getBytes());

    FileContent content = service.download(fileId);

    assertThat(content.filename()).isEqualTo("a.txt");
    assertThat(content.contentType()).isEqualTo("text/plain");
    assertThat(content.resource().getContentAsByteArray()).isEqualTo("hi".getBytes());
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
}
