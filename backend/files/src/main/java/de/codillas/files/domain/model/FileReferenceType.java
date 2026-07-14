package de.codillas.files.domain.model;

import org.springframework.modulith.NamedInterface;

/** What a stored file is attached to. Exposed with the {@code FileAccessAuthorizer} SPI. */
@NamedInterface("reference-type")
public enum FileReferenceType {
  HOMEWORK,
  CHAT,
  MATERIAL
}
