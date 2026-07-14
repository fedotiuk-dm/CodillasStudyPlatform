package de.codillas.enrollment.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.CourseDeleted;

import lombok.RequiredArgsConstructor;

/** Cascades a deleted course to its cohorts (each republishes GroupDeleted). */
@Component
@RequiredArgsConstructor
class EnrollmentEventListener {
  private final GroupService service;

  @ApplicationModuleListener
  void on(CourseDeleted event) {
    service.onCourseDeleted(event);
  }
}
