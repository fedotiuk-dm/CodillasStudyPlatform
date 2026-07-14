package de.codillas.chat.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import de.codillas.files.domain.model.FileReferenceType;
import de.codillas.files.service.FileAccessAuthorizer;

import lombok.RequiredArgsConstructor;

/** A chat attachment is readable only by a member of the room it was posted to. */
@Component
@RequiredArgsConstructor
public class ChatFileAuthorizer implements FileAccessAuthorizer {

  private final ChatService chatService;

  @Override
  public FileReferenceType referenceType() {
    return FileReferenceType.CHAT;
  }

  @Override
  public boolean canAccess(UUID referenceId, UUID uploaderId, UUID userId) {
    return chatService.isMember(referenceId, userId);
  }
}
