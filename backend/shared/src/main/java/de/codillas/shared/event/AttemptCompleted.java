package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a student submits a test attempt. Lives in {@code shared} because it is consumed
 * by more than one module (gradebook records the score, notification alerts the teacher/student).
 */
public record AttemptCompleted(UUID attemptId, UUID testId, UUID studentId, int score) {}
