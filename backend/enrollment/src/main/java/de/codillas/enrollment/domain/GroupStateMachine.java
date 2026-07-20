package de.codillas.enrollment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.shared.domain.StateMachines;
import de.codillas.shared.exception.ConflictException;

/**
 * Declarative state machine for {@link Group}: DRAFT → RUNNING → ARCHIVED, and back — ARCHIVED →
 * RUNNING resumes a retired cohort. ARCHIVED is read-only while it lasts, but it is not terminal:
 * anything derived from it (e.g. homework's muted reminders) must be reversible.
 */
@Component
public class GroupStateMachine {

  private static final Map<GroupStatus, Set<GroupStatus>> ALLOWED =
      Map.of(
          GroupStatus.DRAFT, EnumSet.of(GroupStatus.RUNNING, GroupStatus.ARCHIVED),
          GroupStatus.RUNNING, EnumSet.of(GroupStatus.ARCHIVED),
          GroupStatus.ARCHIVED, EnumSet.of(GroupStatus.RUNNING));

  public void transitionTo(Group group, GroupStatus target) {
    StateMachines.transition("group", group.getStatus(), target, ALLOWED, group::setStatus);
  }

  /** Enrolment and scheduling are rejected once a group is archived. */
  public void assertWritable(Group group) {
    if (group.getStatus() == GroupStatus.ARCHIVED) {
      throw new ConflictException("Cannot modify an ARCHIVED group");
    }
  }
}
