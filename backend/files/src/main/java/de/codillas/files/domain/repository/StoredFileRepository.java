package de.codillas.files.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.files.domain.model.StoredFile;
import de.codillas.shared.domain.BaseAuditableEntity_;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

  /** Newest upload first — the "my files" order. */
  Sort NEWEST_FIRST = Sort.by(Sort.Order.desc(BaseAuditableEntity_.CREATED_AT));

  Page<StoredFile> findByUploadedBy(UUID uploadedBy, Pageable pageable);
}
