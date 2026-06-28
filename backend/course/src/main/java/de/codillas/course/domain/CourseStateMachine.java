package de.codillas.course.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.CourseStatus;
import de.codillas.shared.domain.StateMachines;

/**
 * Declarative state machine for {@link Course}: DRAFT → PUBLISHED → ARCHIVED (DRAFT may also be
 * archived).
 */
@Component
public class CourseStateMachine {

  private static final Map<CourseStatus, Set<CourseStatus>> ALLOWED =
      Map.of(
          CourseStatus.DRAFT, EnumSet.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED),
          CourseStatus.PUBLISHED, EnumSet.of(CourseStatus.ARCHIVED));

  public void transitionTo(Course course, CourseStatus target) {
    StateMachines.transition("course", course.getStatus(), target, ALLOWED, course::setStatus);
  }
}
