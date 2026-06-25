package de.codillas.homework.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.model.SubmissionStatus;
import de.codillas.shared.exception.ConflictException;

/**
 * Declarative state machine for {@link Submission}: DRAFT → SUBMITTED → IN_REVIEW → GRADED /
 * RETURNED. Entities stay anemic — this bean owns the allowed transitions and the editable-while-
 * draft invariant, rejecting anything else with a 409.
 */
@Component
public class SubmissionStateMachine {

  private static final Map<SubmissionStatus, Set<SubmissionStatus>> ALLOWED =
      Map.of(
          SubmissionStatus.DRAFT, EnumSet.of(SubmissionStatus.SUBMITTED),
          SubmissionStatus.SUBMITTED,
              EnumSet.of(
                  SubmissionStatus.IN_REVIEW, SubmissionStatus.GRADED, SubmissionStatus.RETURNED),
          SubmissionStatus.IN_REVIEW,
              EnumSet.of(SubmissionStatus.GRADED, SubmissionStatus.RETURNED));

  public void transitionTo(Submission submission, SubmissionStatus target) {
    Set<SubmissionStatus> allowed =
        ALLOWED.getOrDefault(submission.getStatus(), EnumSet.noneOf(SubmissionStatus.class));
    if (!allowed.contains(target)) {
      throw new ConflictException(
          "Cannot move submission from " + submission.getStatus() + " to " + target);
    }
    submission.setStatus(target);
  }

  /** A submission's content is editable only while it is still a draft. */
  public void assertEditable(Submission submission) {
    if (submission.getStatus() != SubmissionStatus.DRAFT) {
      throw new ConflictException(
          "Only a draft submission can be edited (current status: " + submission.getStatus() + ")");
    }
  }
}
