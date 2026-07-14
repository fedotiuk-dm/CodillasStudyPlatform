package de.codillas.assessment.domain.model;

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
 * A student's single attempt at a test. Unique per (test, student). Anemic — AttemptStateMachine
 * drives status.
 */
@Entity
@Table(
    name = "attempts",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"test_id", "student_id", "attempt_number"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Attempt extends BaseAuditableEntity {

  @Column(name = "test_id", nullable = false)
  private UUID testId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private AttemptStatus status = AttemptStatus.IN_PROGRESS;

  @Column(nullable = false)
  @Builder.Default
  private int score = 0;

  @Column(name = "attempt_number", nullable = false)
  @Builder.Default
  private int attemptNumber = 1;

  @Column(name = "started_at")
  private Instant startedAt;
}
