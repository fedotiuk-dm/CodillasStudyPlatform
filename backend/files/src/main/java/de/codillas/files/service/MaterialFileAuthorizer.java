package de.codillas.files.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.codillas.files.domain.model.FileReferenceType;

/**
 * Course materials are visible to any authenticated caller (download is already
 * {@code @RequiresAuthenticated}).
 */
@Component
public class MaterialFileAuthorizer implements FileAccessAuthorizer {

  @Override
  public FileReferenceType referenceType() {
    return FileReferenceType.MATERIAL;
  }

  @Override
  public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
    return true;
  }
}
