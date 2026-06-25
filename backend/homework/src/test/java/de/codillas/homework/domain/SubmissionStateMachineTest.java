package de.codillas.homework.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.model.SubmissionStatus;
import de.codillas.shared.exception.ConflictException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubmissionStateMachine")
class SubmissionStateMachineTest {

  private final SubmissionStateMachine stateMachine = new SubmissionStateMachine();

  private static Submission inStatus(SubmissionStatus status) {
    return Submission.builder().version(1).status(status).build();
  }

  @Test
  @DisplayName("DRAFT can move to SUBMITTED")
  void draftToSubmitted() {
    Submission submission = inStatus(SubmissionStatus.DRAFT);
    stateMachine.transitionTo(submission, SubmissionStatus.SUBMITTED);
    assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
  }

  @Test
  @DisplayName("SUBMITTED can move to IN_REVIEW, GRADED or RETURNED")
  void submittedTransitions() {
    Submission toReview = inStatus(SubmissionStatus.SUBMITTED);
    stateMachine.transitionTo(toReview, SubmissionStatus.IN_REVIEW);
    assertThat(toReview.getStatus()).isEqualTo(SubmissionStatus.IN_REVIEW);

    assertThatNoException()
        .isThrownBy(
            () ->
                stateMachine.transitionTo(
                    inStatus(SubmissionStatus.SUBMITTED), SubmissionStatus.GRADED));
    assertThatNoException()
        .isThrownBy(
            () ->
                stateMachine.transitionTo(
                    inStatus(SubmissionStatus.SUBMITTED), SubmissionStatus.RETURNED));
  }

  @Test
  @DisplayName("IN_REVIEW can move to GRADED or RETURNED")
  void inReviewTransitions() {
    Submission graded = inStatus(SubmissionStatus.IN_REVIEW);
    stateMachine.transitionTo(graded, SubmissionStatus.GRADED);
    assertThat(graded.getStatus()).isEqualTo(SubmissionStatus.GRADED);
  }

  @Test
  @DisplayName("an illegal transition is a conflict")
  void illegalTransition_conflicts() {
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () ->
                stateMachine.transitionTo(
                    inStatus(SubmissionStatus.DRAFT), SubmissionStatus.GRADED));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(
            () ->
                stateMachine.transitionTo(
                    inStatus(SubmissionStatus.GRADED), SubmissionStatus.SUBMITTED));
  }

  @Test
  @DisplayName("only a draft is editable")
  void assertEditable_onlyDraft() {
    assertThatNoException()
        .isThrownBy(() -> stateMachine.assertEditable(inStatus(SubmissionStatus.DRAFT)));
    assertThatExceptionOfType(ConflictException.class)
        .isThrownBy(() -> stateMachine.assertEditable(inStatus(SubmissionStatus.SUBMITTED)));
  }
}
