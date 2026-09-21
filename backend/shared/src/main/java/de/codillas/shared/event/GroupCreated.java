package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a group is created. Lives in {@code shared} because chat consumes it to open the
 * group channel with the assigned teacher inside before any student is enrolled.
 */
public record GroupCreated(UUID groupId, UUID teacherId) {}
