package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a group (cohort) is hard-deleted. Lives in {@code shared}: gradebook, homework,
 * notification and chat consume it to purge their group-scoped rows.
 */
public record GroupDeleted(UUID groupId) {}
