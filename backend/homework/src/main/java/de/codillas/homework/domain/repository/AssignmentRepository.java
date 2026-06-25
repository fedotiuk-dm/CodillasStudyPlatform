package de.codillas.homework.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.Assignment;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

  Page<Assignment> findByGroupId(UUID groupId, Pageable pageable);
}
