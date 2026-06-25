package de.codillas.homework.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.shared.domain.StateMachines;

/**
 * Declarative state machine for {@link Assignment}. Entities stay anemic — this bean owns the
 * allowed transitions and applies them, rejecting anything else with a 409.
 */
@Component
public class AssignmentStateMachine {

  private static final Map<AssignmentStatus, Set<AssignmentStatus>> ALLOWED =
      Map.of(AssignmentStatus.DRAFT, EnumSet.of(AssignmentStatus.PUBLISHED));

  public void transitionTo(Assignment assignment, AssignmentStatus target) {
    StateMachines.transition(
        "assignment", assignment.getStatus(), target, ALLOWED, assignment::setStatus);
  }
}
