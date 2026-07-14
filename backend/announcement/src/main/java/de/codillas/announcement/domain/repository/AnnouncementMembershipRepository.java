package de.codillas.announcement.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.announcement.domain.model.AnnouncementMembership;

@Repository
public interface AnnouncementMembershipRepository
    extends JpaRepository<AnnouncementMembership, UUID> {

  boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);

  List<AnnouncementMembership> findByStudentId(UUID studentId);

  void deleteByGroupId(UUID groupId);
}
