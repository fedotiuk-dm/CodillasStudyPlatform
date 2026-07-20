package de.codillas.assessment.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.LessonsDeleted;

import lombok.RequiredArgsConstructor;

/** Clears the lesson back-pointer of tests whose lesson was deleted upstream in course. */
@Component
@RequiredArgsConstructor
class AssessmentEventListener {

  private final TestService service;

  @ApplicationModuleListener
  void on(LessonsDeleted event) {
    service.onLessonsDeleted(event.lessonIds());
  }
}
