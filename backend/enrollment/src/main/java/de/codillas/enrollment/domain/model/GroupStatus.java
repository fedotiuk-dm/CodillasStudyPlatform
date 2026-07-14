package de.codillas.enrollment.domain.model;

/** A cohort is DRAFT while set up, RUNNING while active, ARCHIVED (read-only) once finished. */
public enum GroupStatus {
  DRAFT,
  RUNNING,
  ARCHIVED
}
