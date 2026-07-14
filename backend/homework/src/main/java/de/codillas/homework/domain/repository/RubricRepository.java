package de.codillas.homework.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Rubric;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, UUID> {

  Optional<Rubric> findByAssignmentId(UUID assignmentId);
}
