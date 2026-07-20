package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a cohort is archived — a terminal, read-only state. Lives in {@code shared}:
 * homework consumes it to stop chasing students of a finished course about deadlines.
 */
public record GroupArchived(UUID groupId) {}
