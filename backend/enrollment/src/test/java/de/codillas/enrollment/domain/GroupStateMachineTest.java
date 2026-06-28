package de.codillas.enrollment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GroupStateMachine")
class GroupStateMachineTest {

  private final GroupStateMachine stateMachine = new GroupStateMachine();

  private static Group inStatus(GroupStatus status) {
    return Group.builder()
        .name("g")
        .courseId(java.util.UUID.randomUUID())
        .teacherId(java.util.UUID.randomUUID())
        .status(status)
        .build();
  }

  @Test
  @DisplayName("DRAFT can start or be archived; RUNNING can be archived")
  void legalTransitions() {
    Group running = inStatus(GroupStatus.DRAFT);
    stateMachine.transitionTo(running, GroupStatus.RUNNING);
    assertThat(running.getStatus()).isEqualTo(GroupStatus.RUNNING);

    assertThatNoException()
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(GroupStatus.DRAFT), GroupStatus.ARCHIVED));
    assertThatNoException()
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(GroupStatus.RUNNING), GroupStatus.ARCHIVED));
  }

  @Test
  @DisplayName("un-archiving and restarting are conflicts")
  void illegalTransitions_conflict() {
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(GroupStatus.ARCHIVED), GroupStatus.RUNNING));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () -> stateMachine.transitionTo(inStatus(GroupStatus.RUNNING), GroupStatus.DRAFT));
  }

  @Test
  @DisplayName("only a non-archived group is writable")
  void assertWritable_rejectsArchived() {
    assertThatNoException()
        .isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.DRAFT)));
    assertThatNoException()
        .isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.RUNNING)));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> stateMachine.assertWritable(inStatus(GroupStatus.ARCHIVED)));
  }
}
