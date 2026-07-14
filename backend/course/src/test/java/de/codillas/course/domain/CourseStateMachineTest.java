package de.codillas.course.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.codillas.course.domain.model.Course;
import de.codillas.course.domain.model.CourseStatus;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CourseStateMachine")
class CourseStateMachineTest {

  private final CourseStateMachine stateMachine = new CourseStateMachine();

  private static Course inStatus(CourseStatus status) {
    return Course.builder().name("c").status(status).build();
  }

  @Test
  @DisplayName("DRAFT can be published or archived")
  void draftTransitions() {
    Course published = inStatus(CourseStatus.DRAFT);
    stateMachine.transitionTo(published, CourseStatus.PUBLISHED);
    assertThat(published.getStatus()).isEqualTo(CourseStatus.PUBLISHED);

    assertThatNoException()
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(CourseStatus.DRAFT), CourseStatus.ARCHIVED));
  }

  @Test
  @DisplayName("PUBLISHED can be archived")
  void publishedToArchived() {
    Course archived = inStatus(CourseStatus.PUBLISHED);
    stateMachine.transitionTo(archived, CourseStatus.ARCHIVED);
    assertThat(archived.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
  }

  @Test
  @DisplayName("re-publishing, un-archiving and un-publishing are conflicts")
  void illegalTransitions_conflict() {
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () ->
                stateMachine.transitionTo(
                    inStatus(CourseStatus.PUBLISHED), CourseStatus.PUBLISHED));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(CourseStatus.PUBLISHED), CourseStatus.DRAFT));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () ->
                stateMachine.transitionTo(inStatus(CourseStatus.ARCHIVED), CourseStatus.PUBLISHED));
  }
}
