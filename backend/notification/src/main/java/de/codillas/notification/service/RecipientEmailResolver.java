package de.codillas.notification.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a recipient's email address. The notification module only knows user ids (cross-module
 * reads are forbidden), so this is the seam where an email source is plugged in — e.g. a
 * user-profile event carrying the address, or a user-module API. The default implementation has no
 * source yet, so email delivery is skipped until one is wired.
 */
public interface RecipientEmailResolver {

  Optional<String> resolve(UUID userId);
}
