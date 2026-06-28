package de.codillas.enrollment.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.enrollment.domain.model.Group;
import de.codillas.enrollment.domain.model.GroupStatus;
import de.codillas.enrollment.domain.model.Group_;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

  /** Stable display order. */
  Sort BY_NAME = Sort.by(Sort.Order.asc(Group_.NAME));

  List<Group> findByCourseId(UUID courseId);

  List<Group> findByIdIn(Collection<UUID> ids, Sort sort);

  Page<Group> findByStatus(GroupStatus status, Pageable pageable);

  List<Group> findByIdInAndStatusIn(
      Collection<UUID> ids, Collection<GroupStatus> statuses, Sort sort);
}
