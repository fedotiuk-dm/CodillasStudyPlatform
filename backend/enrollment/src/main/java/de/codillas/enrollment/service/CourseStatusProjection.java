package de.codillas.enrollment.service;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import de.codillas.enrollment.domain.model.CourseStatusView;
import de.codillas.enrollment.domain.repository.CourseStatusViewRepository;
import de.codillas.shared.event.CourseArchived;
import de.codillas.shared.event.CourseDeleted;
import de.codillas.shared.event.CoursePublished;

import lombok.RequiredArgsConstructor;

/**
 * Keeps enrollment's local course-status read model current from course lifecycle events. This is
 * the decoupling core: enrollment learns which courses are PUBLISHED by id, via events, instead of
 * depending on the course module. A {@code save} with an assigned id upserts the row.
 */
@Component
@RequiredArgsConstructor
class CourseStatusProjection {

  private final CourseStatusViewRepository repository;

  @ApplicationModuleListener
  void on(CoursePublished event) {
    repository.save(new CourseStatusView(event.courseId(), "PUBLISHED"));
  }

  @ApplicationModuleListener
  void on(CourseArchived event) {
    repository.save(new CourseStatusView(event.courseId(), "ARCHIVED"));
  }

  @ApplicationModuleListener
  void on(CourseDeleted event) {
    repository.deleteById(event.courseId());
  }
}
