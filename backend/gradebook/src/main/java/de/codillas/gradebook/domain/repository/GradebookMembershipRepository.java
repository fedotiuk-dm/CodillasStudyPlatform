package de.codillas.gradebook.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.gradebook.domain.model.GradebookMembership;

@Repository
public interface GradebookMembershipRepository extends JpaRepository<GradebookMembership, UUID> {

  List<GradebookMembership> findByGroupId(UUID groupId);

  boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);
}
