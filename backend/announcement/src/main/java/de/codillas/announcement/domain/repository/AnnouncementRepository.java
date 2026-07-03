package de.codillas.announcement.domain.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.announcement.domain.model.Announcement;
import de.codillas.announcement.domain.model.Announcement_;
import de.codillas.shared.domain.BaseAuditableEntity_;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

  /** Pinned posts first, then newest — the stream order. */
  Sort STREAM_ORDER =
      Sort.by(
          Sort.Order.desc(Announcement_.PINNED), Sort.Order.desc(BaseAuditableEntity_.CREATED_AT));

  Page<Announcement> findByGroupId(UUID groupId, Pageable pageable);

  Page<Announcement> findByGroupIdIn(Collection<UUID> groupIds, Pageable pageable);

  void deleteByGroupId(UUID groupId);
}
