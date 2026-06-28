package de.codillas.course.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.CourseStatus;
import de.codillas.course.domain.repository.CourseRepository;
import de.codillas.shared.event.CoursePublished;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Republishes {@link CoursePublished} on startup for every course already in {@code PUBLISHED}
 * status. Dev-seeded rows and the Liquibase backfill (existing courses set to PUBLISHED) never
 * fired a lifecycle event; this reconciler lets event-fed downstream modules (enrollment) catch up.
 * Downstream consumers are idempotent, so a duplicate emit is safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class CourseStartupReconciler {

  private final CourseRepository courseRepository;
  private final ApplicationEventPublisher events;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional(readOnly = true)
  void republishPublishedCourses() {
    var published = courseRepository.findByStatus(CourseStatus.PUBLISHED);
    published.stream()
        .map(Course::getId)
        .forEach(id -> events.publishEvent(new CoursePublished(id)));
    log.info("Republished CoursePublished for {} already-PUBLISHED course(s)", published.size());
  }
}
