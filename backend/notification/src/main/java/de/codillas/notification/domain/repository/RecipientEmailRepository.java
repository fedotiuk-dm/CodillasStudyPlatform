package de.codillas.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.codillas.notification.domain.model.RecipientEmail;

@Repository
public interface RecipientEmailRepository extends JpaRepository<RecipientEmail, UUID> {

  Optional<RecipientEmail> findByUserId(UUID userId);
}
