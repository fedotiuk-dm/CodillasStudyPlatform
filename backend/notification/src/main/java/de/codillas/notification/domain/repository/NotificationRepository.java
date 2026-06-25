package de.codillas.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
