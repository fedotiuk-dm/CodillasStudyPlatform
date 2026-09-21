package de.codillas.homework.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.model.Submission_;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

  /** Newest version first — find a student's latest attempt. */
  Sort LATEST_VERSION = Sort.by(Sort.Order.desc(Submission_.VERSION));

  /** Grouped by student, then version — the teacher's review list. */
  Sort BY_STUDENT_THEN_VERSION = Sort.by(Submission_.STUDENT_ID, Submission_.VERSION);

  List<Submission> findByAssignmentId(UUID assignmentId, Sort sort);

  List<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId, Sort sort);

  List<Submission> findByAssignmentIdIn(Collection<UUID> assignmentIds);

  void deleteByAssignmentIdIn(Collection<UUID> assignmentIds);

  /**
   * Latest version for a student on an assignment, used to compute the next version number. {@code
   * First} limits to the top row; the {@link Sort} arg supplies the ordering (no OrderBy in the
   * name).
   */
  Optional<Submission> findFirstByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId, Sort sort);
}
