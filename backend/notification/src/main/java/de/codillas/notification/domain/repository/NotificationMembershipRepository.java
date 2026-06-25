package de.codillas.notification.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.notification.domain.model.NotificationMembership;

@Repository
public interface NotificationMembershipRepository
    extends JpaRepository<NotificationMembership, UUID> {

  List<NotificationMembership> findByGroupId(UUID groupId);

  boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);
}
