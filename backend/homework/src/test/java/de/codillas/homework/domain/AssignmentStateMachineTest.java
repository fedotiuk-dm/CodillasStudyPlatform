package de.codillas.homework.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssignmentStateMachine")
class AssignmentStateMachineTest {

  private final AssignmentStateMachine stateMachine = new AssignmentStateMachine();

  @Test
  @DisplayName("DRAFT can move to PUBLISHED")
  void draftToPublished() {
    Assignment assignment = Assignment.builder().title("HW1").build();
    stateMachine.transitionTo(assignment, AssignmentStatus.PUBLISHED);
    assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.PUBLISHED);
  }

  @Test
  @DisplayName("publishing an already-published assignment is a conflict")
  void publishTwice_conflicts() {
    Assignment assignment =
        Assignment.builder().title("HW1").status(AssignmentStatus.PUBLISHED).build();
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> stateMachine.transitionTo(assignment, AssignmentStatus.PUBLISHED));
  }
}
