package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a user's email is captured/changed at provisioning. Lives in {@code shared}:
 * notification consumes it into its recipient_email read model.
 */
public record UserEmailChanged(UUID userId, String email) {}
