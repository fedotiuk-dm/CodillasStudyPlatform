package de.codillas.shared.event;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Published when a student submits a test attempt. Lives in {@code shared} because it is consumed
 * by more than one module (gradebook records the grade, notification alerts the teacher/student).
 *
 * <p>{@code awarded} is the attempt score; {@code maxPoints} is the sum of the test's question
 * points (gradebook weights {@code awarded/maxPoints}). {@code groupId} may be {@code null}: a test
 * is lesson-scoped and reused across cohorts, so assessment cannot know the student's group —
 * gradebook backfills it from its own membership read model.
 */
public record AttemptCompleted(
    UUID attemptId,
    UUID testId,
    UUID studentId,
    int awarded,
    int maxPoints,
    @Nullable UUID groupId) {}
