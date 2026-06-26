package de.codillas.enrollment.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.Membership;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  List<Membership> findByGroupId(UUID groupId);

  List<Membership> findByUserId(UUID userId);
}
