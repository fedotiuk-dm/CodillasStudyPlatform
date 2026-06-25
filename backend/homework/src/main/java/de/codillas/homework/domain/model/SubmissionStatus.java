package de.codillas.homework.domain.model;

/** Lifecycle of a single submission version: DRAFT → SUBMITTED → IN_REVIEW → GRADED / RETURNED. */
public enum SubmissionStatus {
  DRAFT,
  SUBMITTED,
  IN_REVIEW,
  GRADED,
  RETURNED
}
