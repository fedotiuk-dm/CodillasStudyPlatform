package de.codillas.homework.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.homework.domain.model.ArchivedGroup;

@Repository
public interface ArchivedGroupRepository extends JpaRepository<ArchivedGroup, UUID> {

  boolean existsByGroupId(UUID groupId);

  void deleteByGroupId(UUID groupId);

  /** The archived subset of the groups the reminder job is about to consider. */
  List<ArchivedGroup> findByGroupIdIn(Collection<UUID> groupIds);
}
