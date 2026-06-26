package de.codillas.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by a scheduled job when a published assignment's deadline is approaching (within the
 * next 24h). Lives in {@code shared} because it is consumed by another module (notification reminds
 * the group members).
 */
public record AssignmentDueSoon(UUID assignmentId, UUID groupId, Instant dueAt) {}
