package de.codillas.assessment.domain.model;

/** A test is editable while DRAFT; once PUBLISHED students may attempt it. */
public enum TestStatus {
  DRAFT,
  PUBLISHED
}
