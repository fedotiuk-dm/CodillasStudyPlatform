package de.codillas.gradebook.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.event.StudentEnrolled;
import de.codillas.shared.event.SubmissionGraded;

import lombok.RequiredArgsConstructor;

/** Feeds the gradebook read model from domain events (durable async delivery via the registry). */
@Component
@RequiredArgsConstructor
class GradebookEventListener {

  private final GradebookService service;

  @ApplicationModuleListener
  void on(StudentEnrolled event) {
    service.recordEnrollment(event);
  }

  @ApplicationModuleListener
  void on(SubmissionGraded event) {
    service.recordSubmissionGrade(event);
  }

  @ApplicationModuleListener
  void on(AttemptCompleted event) {
    service.recordAttempt(event);
  }
}
