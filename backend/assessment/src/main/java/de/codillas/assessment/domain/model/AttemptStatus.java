package de.codillas.assessment.domain.model;

/** Attempt lifecycle: IN_PROGRESS → SUBMITTED (awaiting manual grading) → GRADED. */
public enum AttemptStatus {
  IN_PROGRESS,
  SUBMITTED,
  GRADED
}
