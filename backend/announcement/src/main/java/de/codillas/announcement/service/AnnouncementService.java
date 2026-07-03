package de.codillas.announcement.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.announcement.api.dto.AnnouncementListResponse;
import de.codillas.announcement.api.dto.AnnouncementResponse;
import de.codillas.announcement.api.dto.CreateAnnouncementRequest;
import de.codillas.announcement.api.dto.UpdateAnnouncementRequest;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.event.StudentEnrolled;

public interface AnnouncementService {

  AnnouncementListResponse listGroupAnnouncements(UUID groupId, Pageable pageable);

  AnnouncementListResponse listMyAnnouncements(Pageable pageable);

  AnnouncementResponse create(UUID groupId, CreateAnnouncementRequest request);

  AnnouncementResponse update(UUID announcementId, UpdateAnnouncementRequest request);

  void delete(UUID announcementId);

  void onStudentEnrolled(StudentEnrolled event);

  void onGroupDeleted(GroupDeleted event);
}
