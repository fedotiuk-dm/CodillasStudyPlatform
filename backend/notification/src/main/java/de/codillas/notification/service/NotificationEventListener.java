package de.codillas.notification.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.AssignmentDueSoon;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.DirectMessagePosted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

import lombok.RequiredArgsConstructor;

/** Turns domain events into notifications (durable async delivery via the publication registry). */
@Component
@RequiredArgsConstructor
class NotificationEventListener {

  private final NotificationService service;

  @ApplicationModuleListener
  void on(StudentEnrolled event) {
    service.onStudentEnrolled(event);
  }

  @ApplicationModuleListener
  void on(AssignmentPublished event) {
    service.onAssignmentPublished(event);
  }

  @ApplicationModuleListener
  void on(AssignmentDueSoon event) {
    service.onAssignmentDueSoon(event);
  }

  @ApplicationModuleListener
  void on(SubmissionGraded event) {
    service.onSubmissionGraded(event);
  }

  @ApplicationModuleListener
  void on(AttemptCompleted event) {
    service.onAttemptCompleted(event);
  }

  @ApplicationModuleListener
  void on(DirectMessagePosted event) {
    service.onDirectMessage(event);
  }
}
