package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a teacher grades a submission. Lives in {@code shared} because it is consumed by
 * more than one module (gradebook records the grade, notification alerts the student).
 *
 * <p>{@code awarded} is the effective score after late penalties; {@code maxPoints} is the
 * denominator (gradebook weights {@code awarded/maxPoints}). {@code groupId} is the assignment's
 * group and is always populated by homework.
 */
public record SubmissionGraded(
    UUID submissionId,
    UUID assignmentId,
    UUID studentId,
    int awarded,
    int maxPoints,
    UUID groupId) {}
