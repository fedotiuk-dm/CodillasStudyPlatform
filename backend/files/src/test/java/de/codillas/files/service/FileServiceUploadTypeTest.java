package de.codillas.files.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.config.FilesProperties;
import de.codillas.files.domain.repository.StoredFileRepository;
import de.codillas.files.mapper.StoredFileMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileServiceImpl — content-type allow-list")
class FileServiceUploadTypeTest {

  @Mock StoredFileRepository repository;
  @Mock ObjectStorage storage;
  @Mock StoredFileMapper mapper;
  @Mock CurrentUser currentUser;
  @Mock ApplicationEventPublisher events;
  FileServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new FileServiceImpl(
            repository,
            storage,
            mapper,
            currentUser,
            List.of(),
            new FilesProperties("b", "e", "r", "a", "s", List.of("application/pdf")),
            events);
  }

  @Test
  @DisplayName("rejects a disallowed content type with 400")
  void rejectsDisallowed() {
    MockMultipartFile bad =
        new MockMultipartFile("file", "x.exe", "application/x-msdownload", new byte[] {1});
    assertThatThrownBy(
            () ->
                service.upload(
                    bad, FileReferenceType.MATERIAL, UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(BadRequestException.class);
    verifyNoInteractions(storage);
  }
}
