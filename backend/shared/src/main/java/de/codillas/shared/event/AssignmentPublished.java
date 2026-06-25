package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a teacher publishes an assignment to a group. Lives in {@code shared} because it
 * is consumed by more than one module (notification alerts the students, chat posts to the
 * channel).
 */
public record AssignmentPublished(UUID assignmentId, UUID groupId) {}
