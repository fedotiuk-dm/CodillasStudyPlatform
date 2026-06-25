package de.codillas.homework.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

  List<Submission> findByAssignmentId(UUID assignmentId, Sort sort);

  /**
   * Latest version for a student on an assignment, used to compute the next version number. {@code
   * First} limits to the top row; the {@link Sort} arg supplies the ordering (no OrderBy in the
   * name).
   */
  Optional<Submission> findFirstByAssignmentIdAndStudentId(
      UUID assignmentId, UUID studentId, Sort sort);
}
