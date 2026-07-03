package de.codillas.announcement.web;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.announcement.api.AnnouncementApi;
import de.codillas.announcement.api.dto.AnnouncementListResponse;
import de.codillas.announcement.api.dto.AnnouncementResponse;
import de.codillas.announcement.api.dto.CreateAnnouncementRequest;
import de.codillas.announcement.api.dto.UpdateAnnouncementRequest;
import de.codillas.announcement.service.AnnouncementService;
import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.shared.security.RequiresTeacher;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link AnnouncementApi}. */
@RestController
@RequiredArgsConstructor
public class AnnouncementController implements AnnouncementApi {

  private final AnnouncementService service;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<AnnouncementListResponse> listGroupAnnouncements(
      UUID groupId, Pageable pageable) {
    return ResponseEntity.ok(service.listGroupAnnouncements(groupId, pageable));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<AnnouncementResponse> createAnnouncement(
      UUID groupId, CreateAnnouncementRequest createAnnouncementRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(groupId, createAnnouncementRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<AnnouncementResponse> updateAnnouncement(
      UUID announcementId, UpdateAnnouncementRequest updateAnnouncementRequest) {
    return ResponseEntity.ok(service.update(announcementId, updateAnnouncementRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<Void> deleteAnnouncement(UUID announcementId) {
    service.delete(announcementId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<AnnouncementListResponse> listMyAnnouncements(Pageable pageable) {
    return ResponseEntity.ok(service.listMyAnnouncements(pageable));
  }
}
