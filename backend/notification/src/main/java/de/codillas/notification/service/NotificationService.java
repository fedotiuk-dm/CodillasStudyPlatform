package de.codillas.notification.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.notification.api.dto.NotificationListResponse;
import de.codillas.shared.event.AssignmentDueSoon;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

public interface NotificationService {

  NotificationListResponse listMyNotifications(UUID userId, Pageable pageable);

  void markRead(UUID userId, UUID notificationId);

  void onStudentEnrolled(StudentEnrolled event);

  void onAssignmentPublished(AssignmentPublished event);

  void onAssignmentDueSoon(AssignmentDueSoon event);

  void onSubmissionGraded(SubmissionGraded event);

  void onAttemptCompleted(AttemptCompleted event);
}
