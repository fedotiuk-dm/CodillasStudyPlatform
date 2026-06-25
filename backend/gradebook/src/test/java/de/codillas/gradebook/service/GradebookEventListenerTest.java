package de.codillas.gradebook.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

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
@DisplayName("GradebookEventListener")
class GradebookEventListenerTest {

  @Mock private GradebookService service;
  @InjectMocks private GradebookEventListener listener;

  @Test
  @DisplayName("delegates each domain event to the matching service method")
  void delegates() {
    StudentEnrolled enrolled = new StudentEnrolled(UUID.randomUUID(), UUID.randomUUID());
    SubmissionGraded graded =
        new SubmissionGraded(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10);
    AttemptCompleted completed =
        new AttemptCompleted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 20);

    listener.on(enrolled);
    listener.on(graded);
    listener.on(completed);

    verify(service).recordEnrollment(enrolled);
    verify(service).recordSubmissionGrade(graded);
    verify(service).recordAttempt(completed);
  }
}
