package de.codillas.enrollment.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.LessonsDeleted;

import lombok.RequiredArgsConstructor;

/**
 * Cascades a deleted course to its cohorts (each republishes GroupDeleted) and clears scheduled
 * sessions that pointed at deleted lessons.
 */
@Component
@RequiredArgsConstructor
class EnrollmentEventListener {
  private final GroupService service;
  private final ScheduledLessonService scheduledLessonService;

  @ApplicationModuleListener
  void on(CourseDeleted event) {
    service.onCourseDeleted(event);
  }

  @ApplicationModuleListener
  void on(LessonsDeleted event) {
    scheduledLessonService.onLessonsDeleted(event.lessonIds());
  }
}
