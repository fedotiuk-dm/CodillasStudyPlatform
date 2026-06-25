package de.codillas.assessment.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.assessment.domain.model.Attempt;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, UUID> {

  boolean existsByTestIdAndStudentId(UUID testId, UUID studentId);
}
