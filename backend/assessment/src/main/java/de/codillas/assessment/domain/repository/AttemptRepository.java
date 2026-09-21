package de.codillas.assessment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.assessment.domain.model.Attempt_;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

  /** Grading order: one student's attempts together, oldest first. */
  Sort BY_STUDENT_THEN_NUMBER =
      Sort.by(Sort.Order.asc(Attempt_.STUDENT_ID), Sort.Order.asc(Attempt_.ATTEMPT_NUMBER));

  /** Every attempt at a test (staff grading view). */
  List<Attempt> findByTestId(UUID testId, Sort sort);

  /** All of a student's attempts at a test (for the cap count + next number). */
  List<Attempt> findByTestIdAndStudentId(UUID testId, UUID studentId);

  /** The student's resumable (in-progress) attempt, if any. */
  Optional<Attempt> findByTestIdAndStudentIdAndStatus(
      UUID testId, UUID studentId, AttemptStatus status);

  /** The student's most recent attempt — its id seeds the taker shuffle. */
  Optional<Attempt> findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(
      UUID testId, UUID studentId);
}
