package de.codillas.enrollment.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.ScheduledLesson;
import de.codillas.enrollment.domain.model.ScheduledLesson_;

@Repository
public interface ScheduledLessonRepository extends JpaRepository<ScheduledLesson, UUID> {

  /** Earliest scheduled lesson first. */
  Sort BY_TIME = Sort.by(Sort.Order.asc(ScheduledLesson_.SCHEDULED_AT));

  /**
   * Ordering comes from the {@link Sort} arg, so the method name stays short (no OrderBy chain).
   */
  List<ScheduledLesson> findByGroupId(UUID groupId, Sort sort);

  List<ScheduledLesson> findByGroupIdIn(Collection<UUID> groupIds, Sort sort);

  /** Sessions still pointing at lessons that were deleted upstream in course. */
  List<ScheduledLesson> findByLessonIdIn(Collection<UUID> lessonIds);
}
