package de.codillas.homework.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A piece of homework a teacher sets for a group. Starts as a draft, then is published. */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Assignment extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "due_at")
  private Instant dueAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private AssignmentStatus status = AssignmentStatus.DRAFT;

  /** Guard so the deadline reminder fires at most once per assignment. */
  @Column(name = "due_reminder_sent", nullable = false)
  @Builder.Default
  private boolean dueReminderSent = false;
}
