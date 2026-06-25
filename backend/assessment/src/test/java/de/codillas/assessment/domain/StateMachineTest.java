package de.codillas.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

@DisplayName("Assessment state machines")
class StateMachineTest {

  @Nested
  @DisplayName("TestStateMachine")
  class TestSm {
    private final TestStateMachine sm = new TestStateMachine();

    @org.junit.jupiter.api.Test
    @DisplayName("DRAFT publishes; double publish conflicts")
    void publish() {
      Test test = Test.builder().title("t").build();
      sm.transitionTo(test, TestStatus.PUBLISHED);
      assertThat(test.getStatus()).isEqualTo(TestStatus.PUBLISHED);
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> sm.transitionTo(test, TestStatus.PUBLISHED));
    }
  }

  @Nested
  @DisplayName("AttemptStateMachine")
  class AttemptSm {
    private final AttemptStateMachine sm = new AttemptStateMachine();

    @org.junit.jupiter.api.Test
    @DisplayName("IN_PROGRESS submits or grades; SUBMITTED grades; bad moves conflict")
    void transitions() {
      Attempt a = Attempt.builder().build();
      sm.transitionTo(a, AttemptStatus.SUBMITTED);
      assertThat(a.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
      sm.transitionTo(a, AttemptStatus.GRADED);
      assertThat(a.getStatus()).isEqualTo(AttemptStatus.GRADED);

      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> sm.transitionTo(Attempt.builder().build(), AttemptStatus.IN_PROGRESS));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("assertInProgress passes only while in progress")
    void assertInProgress() {
      assertThatNoException().isThrownBy(() -> sm.assertInProgress(Attempt.builder().build()));
      Attempt submitted = Attempt.builder().status(AttemptStatus.SUBMITTED).build();
      assertThatExceptionOfType(ConflictException.class)
          .isThrownBy(() -> sm.assertInProgress(submitted));
    }
  }
}
