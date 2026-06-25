package de.codillas.enrollment.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A student's attendance for a scheduled lesson. Unique per (lesson, user). */
@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"scheduled_lesson_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Attendance extends BaseAuditableEntity {

  @Column(name = "scheduled_lesson_id", nullable = false)
  private UUID scheduledLessonId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private boolean present;
}
