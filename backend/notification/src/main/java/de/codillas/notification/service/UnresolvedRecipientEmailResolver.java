package de.codillas.notification.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Default {@link RecipientEmailResolver}: no email source is wired yet, so it resolves nothing and
 * email is skipped (in-app delivery still works). Replace this bean once an address source exists.
 */
@Component
class UnresolvedRecipientEmailResolver implements RecipientEmailResolver {

  @Override
  public Optional<String> resolve(UUID userId) {
    return Optional.empty();
  }
}
