package de.codillas.notification.web;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.notification.api.NotificationApi;
import de.codillas.notification.api.dto.NotificationListResponse;
import de.codillas.notification.service.NotificationService;
import de.codillas.shared.security.CurrentUser;
import de.codillas.shared.security.RequiresAuthenticated;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link NotificationApi}. */
@RestController
@RequiredArgsConstructor
public class NotificationController implements NotificationApi {

  private final NotificationService notificationService;
  private final CurrentUser currentUser;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<NotificationListResponse> listMyNotifications(Pageable pageable) {
    return ResponseEntity.ok(notificationService.listMyNotifications(currentUser.id(), pageable));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<Void> markRead(UUID notificationId) {
    notificationService.markRead(currentUser.id(), notificationId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<Void> markAllRead() {
    notificationService.markAllRead(currentUser.id());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
