package de.codillas.assessment.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Attempt;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

  /**
   * One attempt per (test, student): used to resume an existing attempt instead of creating a new
   * one.
   */
  Optional<Attempt> findByTestIdAndStudentId(UUID testId, UUID studentId);
}
