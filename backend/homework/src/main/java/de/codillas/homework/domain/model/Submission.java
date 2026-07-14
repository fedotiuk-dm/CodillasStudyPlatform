package de.codillas.homework.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A student's attempt at an assignment. Submissions are append-only versions per (assignment,
 * student): each retry is a new row with the next version. Transitions are owned by {@code
 * SubmissionStateMachine}, not this entity.
 *
 * <p>Content is inline text for now; file attachments arrive with the files module (a fileId
 * column).
 */
@Entity
@Table(
    name = "submissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "student_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Submission extends BaseAuditableEntity {

  @Column(name = "assignment_id", nullable = false)
  private UUID assignmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(nullable = false)
  private int version;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private SubmissionStatus status = SubmissionStatus.DRAFT;

  @Column(columnDefinition = "text")
  private String content;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(nullable = false)
  @Builder.Default
  private boolean late = false;
}
