package de.codillas.files.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Metadata for a file stored in object storage (S3/minio). The bytes live under {@code storageKey}.
 */
@Entity
@Table(name = "stored_files")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class StoredFile extends BaseAuditableEntity {

  @Column(name = "storage_key", nullable = false, unique = true, length = 200)
  private String storageKey;

  @Column(name = "original_filename", nullable = false)
  private String originalFilename;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "uploaded_by", nullable = false)
  private UUID uploadedBy;

  @Enumerated(EnumType.STRING)
  @Column(name = "reference_type", length = 20)
  private FileReferenceType referenceType;

  @Column(name = "reference_id")
  private UUID referenceId;
}
