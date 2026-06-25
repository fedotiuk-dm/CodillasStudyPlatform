package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a student is enrolled into a group. Lives in {@code shared} because it is consumed
 * by more than one module (gradebook initialises progress, chat adds the student to the channel).
 */
public record StudentEnrolled(UUID groupId, UUID userId) {}
