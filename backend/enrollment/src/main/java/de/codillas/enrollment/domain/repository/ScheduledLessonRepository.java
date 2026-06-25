package de.codillas.enrollment.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.ScheduledLesson;

@Repository
public interface ScheduledLessonRepository extends JpaRepository<ScheduledLesson, UUID> {

  /**
   * Ordering comes from the {@link Sort} arg, so the method name stays short (no OrderBy chain).
   */
  List<ScheduledLesson> findByGroupId(UUID groupId, Sort sort);
}
