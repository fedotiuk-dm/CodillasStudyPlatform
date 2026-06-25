package de.codillas.assessment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.shared.exception.ConflictException;

/** Declarative state machine for {@link Test}: DRAFT → PUBLISHED. */
@Component
public class TestStateMachine {

  private static final Map<TestStatus, Set<TestStatus>> ALLOWED =
      Map.of(TestStatus.DRAFT, EnumSet.of(TestStatus.PUBLISHED));

  public void transitionTo(Test test, TestStatus target) {
    Set<TestStatus> allowed =
        ALLOWED.getOrDefault(test.getStatus(), EnumSet.noneOf(TestStatus.class));
    if (!allowed.contains(target)) {
      throw new ConflictException("Cannot move test from " + test.getStatus() + " to " + target);
    }
    test.setStatus(target);
  }
}
