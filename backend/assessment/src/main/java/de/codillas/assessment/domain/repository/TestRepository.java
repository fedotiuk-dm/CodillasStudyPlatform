package de.codillas.assessment.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;

@Repository
public interface TestRepository extends JpaRepository<Test, UUID> {

  Page<Test> findByLessonId(UUID lessonId, Pageable pageable);

  Page<Test> findByStatus(TestStatus status, Pageable pageable);

  Page<Test> findByLessonIdAndStatus(UUID lessonId, TestStatus status, Pageable pageable);

  /** Tests still pointing at lessons that were deleted upstream in course. */
  List<Test> findByLessonIdIn(Collection<UUID> lessonIds);
}
