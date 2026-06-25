package de.codillas.files.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.files.domain.model.StoredFile;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {}
