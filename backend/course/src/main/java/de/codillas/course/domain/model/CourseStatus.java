package de.codillas.course.domain.model;

/**
 * A course is editable while DRAFT, selectable for groups once PUBLISHED, retired when ARCHIVED.
 */
public enum CourseStatus {
  DRAFT,
  PUBLISHED,
  ARCHIVED
}
