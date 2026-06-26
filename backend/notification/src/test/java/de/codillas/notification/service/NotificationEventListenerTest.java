package de.codillas.notification.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import de.codillas.shared.event.AssignmentDueSoon;
import de.codillas.shared.event.AssignmentPublished;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

  @Mock private NotificationService service;
  @InjectMocks private NotificationEventListener listener;

  @Test
  @DisplayName("delegates each domain event to the matching service method")
  void delegates() {
    StudentEnrolled enrolled = new StudentEnrolled(UUID.randomUUID(), UUID.randomUUID());
    AssignmentPublished published = new AssignmentPublished(UUID.randomUUID(), UUID.randomUUID());
    AssignmentDueSoon dueSoon =
        new AssignmentDueSoon(UUID.randomUUID(), UUID.randomUUID(), java.time.Instant.now());
    SubmissionGraded graded =
        new SubmissionGraded(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 7);
    AttemptCompleted completed =
        new AttemptCompleted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 9);

    listener.on(enrolled);
    listener.on(published);
    listener.on(dueSoon);
    listener.on(graded);
    listener.on(completed);

    verify(service).onStudentEnrolled(enrolled);
    verify(service).onAssignmentPublished(published);
    verify(service).onAssignmentDueSoon(dueSoon);
    verify(service).onSubmissionGraded(graded);
    verify(service).onAttemptCompleted(completed);
  }
}
