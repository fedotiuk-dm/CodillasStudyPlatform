package de.codillas.enrollment.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.shared.event.CourseArchived;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.LessonsDeleted;

import lombok.RequiredArgsConstructor;

/**
 * Cascades course-level lifecycle onto the cohorts running it: delete removes them (each
 * republishing GroupDeleted), archive retires them, and deleted lessons are unlinked from the
 * scheduled sessions that pointed at them.
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
  void on(CourseArchived event) {
    service.onCourseArchived(event);
  }

  @ApplicationModuleListener
  void on(LessonsDeleted event) {
    scheduledLessonService.onLessonsDeleted(event.lessonIds());
  }
}
