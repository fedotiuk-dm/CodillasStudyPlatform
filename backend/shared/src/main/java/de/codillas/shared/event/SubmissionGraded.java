package de.codillas.shared.event;

import java.util.UUID;

/**
 * Published when a teacher grades a submission. Lives in {@code shared} because it is consumed by
 * more than one module (gradebook records the score, notification alerts the student).
 */
public record SubmissionGraded(UUID submissionId, UUID assignmentId, UUID studentId, int score) {}
