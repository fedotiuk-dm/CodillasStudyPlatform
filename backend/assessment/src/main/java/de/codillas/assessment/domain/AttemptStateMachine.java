package de.codillas.assessment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.shared.exception.ConflictException;

/** Declarative state machine for {@link Attempt}: IN_PROGRESS → SUBMITTED → GRADED. */
@Component
public class AttemptStateMachine {

  private static final Map<AttemptStatus, Set<AttemptStatus>> ALLOWED =
      Map.of(
          AttemptStatus.IN_PROGRESS, EnumSet.of(AttemptStatus.SUBMITTED, AttemptStatus.GRADED),
          AttemptStatus.SUBMITTED, EnumSet.of(AttemptStatus.GRADED));

  public void transitionTo(Attempt attempt, AttemptStatus target) {
    Set<AttemptStatus> allowed =
        ALLOWED.getOrDefault(attempt.getStatus(), EnumSet.noneOf(AttemptStatus.class));
    if (!allowed.contains(target)) {
      throw new ConflictException(
          "Cannot move attempt from " + attempt.getStatus() + " to " + target);
    }
    attempt.setStatus(target);
  }

  /** An attempt can only be answered while it is still in progress. */
  public void assertInProgress(Attempt attempt) {
    if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
      throw new ConflictException(
          "Attempt is no longer in progress (status: " + attempt.getStatus() + ")");
    }
  }
}
