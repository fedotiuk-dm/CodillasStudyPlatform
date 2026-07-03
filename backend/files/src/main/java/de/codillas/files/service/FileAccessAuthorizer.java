package de.codillas.files.service;

import java.util.UUID;

import org.springframework.modulith.NamedInterface;

import de.codillas.files.domain.model.FileReferenceType;

/**
 * SPI: the owning module decides whether a caller may read a stored file of a given reference type.
 * Resolved by {@link #referenceType()} at download time and implemented once per module that owns
 * files (homework, chat, files/materials). Receives only ids — never the {@code StoredFile} entity
 * — so an implementing module imports no files persistence type.
 *
 * <p>{@code @NamedInterface}: this SPI (and the {@link FileReferenceType} it needs) is the files
 * module's exposed surface for implementers — Modulith's {@code verify()} allows other modules to
 * depend on it.
 */
@NamedInterface("spi")
public interface FileAccessAuthorizer {

  /** The reference type this authorizer governs. */
  FileReferenceType referenceType();

  /**
   * @param referenceId what the file is attached to (e.g. a submission id, a chat room id), may be
   *     null
   * @param uploaderId the user who uploaded the file
   * @param userId the calling user
   * @return true if the caller may read the file
   */
  boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId);
}
