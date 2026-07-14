package de.codillas.enrollment.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Enrollment's own read model of a course's lifecycle status, kept current by consuming the course
 * module's lifecycle events ({@code CoursePublished}/{@code CourseArchived}/{@code CourseDeleted}).
 * Lets group-create check, by id, that a course is PUBLISHED without enrollment depending on the
 * course module (no codillas-course dependency, no de.codillas.course.* import).
 */
@Entity
@Table(name = "course_status_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStatusView {

  /** The course's id — also the natural primary key of this read model. */
  @Id
  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  /** Mirrors the course's status as a plain string (PUBLISHED / ARCHIVED). */
  @Column(nullable = false, length = 20)
  private String status;
}
