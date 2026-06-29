package de.codillas.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.codillas.notification.domain.model.Notification;
import de.codillas.shared.domain.BaseAuditableEntity_;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  /** Newest first — the inbox order. */
  Sort NEWEST_FIRST = Sort.by(Sort.Order.desc(BaseAuditableEntity_.CREATED_AT));

  Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);

  long countByRecipientIdAndReadFalse(UUID recipientId);

  Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

  /** Bulk mark-all-read for one recipient — one UPDATE, no per-row load. */
  @Modifying
  @Query(
      "update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
  void markAllReadByRecipientId(@Param("recipientId") UUID recipientId);
}
