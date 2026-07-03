package de.codillas.files.mapper;

import java.util.UUID;

import org.springframework.data.domain.Page;

import de.codillas.files.api.dto.FileListResponse;
import de.codillas.files.api.dto.FileReferenceType;
import de.codillas.files.api.dto.StoredFileResponse;
import de.codillas.files.domain.model.StoredFile;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface StoredFileMapper {

  StoredFileResponse toResponse(StoredFile entity);

  FileListResponse toListResponse(Page<StoredFile> page);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  StoredFile toEntity(
      String storageKey,
      String originalFilename,
      String contentType,
      long fileSize,
      UUID uploadedBy,
      FileReferenceType referenceType,
      UUID referenceId);
}
