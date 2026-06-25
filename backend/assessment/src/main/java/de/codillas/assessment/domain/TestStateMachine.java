package de.codillas.assessment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.shared.domain.StateMachines;

/** Declarative state machine for {@link Test}: DRAFT → PUBLISHED. */
@Component
public class TestStateMachine {

  private static final Map<TestStatus, Set<TestStatus>> ALLOWED =
      Map.of(TestStatus.DRAFT, EnumSet.of(TestStatus.PUBLISHED));

  public void transitionTo(Test test, TestStatus target) {
    StateMachines.transition("test", test.getStatus(), target, ALLOWED, test::setStatus);
  }
}
