package de.codillas.homework.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.codillas.files.domain.model.FileReferenceType;
import de.codillas.files.service.FileAccessAuthorizer;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/** A homework attachment is readable by the student who uploaded it (its owner) or any staff. */
@Component
@RequiredArgsConstructor
public class HomeworkFileAuthorizer implements FileAccessAuthorizer {

  private final CurrentUser currentUser;

  @Override
  public FileReferenceType referenceType() {
    return FileReferenceType.HOMEWORK;
  }

  @Override
  public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
    return userId.equals(uploaderId) || currentUser.isStaff();
  }
}
