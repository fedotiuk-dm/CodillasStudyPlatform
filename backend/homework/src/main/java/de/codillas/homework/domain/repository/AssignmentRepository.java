package de.codillas.homework.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.model.Assignment_;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

  /** Soonest deadline first — the order the reminder job processes due assignments in. */
  Sort BY_DUE_AT = Sort.by(Sort.Order.asc(Assignment_.DUE_AT));

  Page<Assignment> findByGroupId(UUID groupId, Pageable pageable);

  List<Assignment> findByGroupId(UUID groupId);

  void deleteByGroupId(UUID groupId);

  /** Assignments still pointing at lessons that were deleted upstream in course. */
  List<Assignment> findByLessonIdIn(Collection<UUID> lessonIds);

  /**
   * Assignments whose deadline reminder is now due: a given status, not yet reminded, and a {@code
   * dueAt} within the {@code [from, to]} window. The {@link Sort} arg supplies the ordering (no
   * OrderBy in the name).
   */
  List<Assignment> findByStatusAndDueReminderSentFalseAndDueAtBetween(
      AssignmentStatus status, Instant from, Instant to, Sort sort);
}
