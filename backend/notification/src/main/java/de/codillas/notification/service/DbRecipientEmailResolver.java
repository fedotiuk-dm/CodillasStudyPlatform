package de.codillas.notification.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.notification.domain.model.RecipientEmail;
import de.codillas.notification.domain.repository.RecipientEmailRepository;
import de.codillas.shared.event.UserEmailChanged;

import lombok.RequiredArgsConstructor;

/**
 * Resolves recipient addresses from a {@code recipient_email} read model fed by {@link
 * UserEmailChanged}. Owns the read model both ways: records on the event, resolves on demand.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DbRecipientEmailResolver implements RecipientEmailResolver {
  private final RecipientEmailRepository repository;

  @Override
  public Optional<String> resolve(UUID userId) {
    return repository.findByUserId(userId).map(RecipientEmail::getEmail);
  }

  @Transactional
  void record(UserEmailChanged event) {
    RecipientEmail row =
        repository
            .findByUserId(event.userId())
            .map(
                existing -> {
                  existing.setEmail(event.email());
                  return existing;
                })
            .orElseGet(
                () -> RecipientEmail.builder().userId(event.userId()).email(event.email()).build());
    repository.save(row);
  }
}
