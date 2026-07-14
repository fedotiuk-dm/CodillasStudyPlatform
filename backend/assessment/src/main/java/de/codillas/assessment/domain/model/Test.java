package de.codillas.assessment.domain.model;

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

/**
 * An interactive test/control a teacher builds for a lesson. Anemic — lifecycle in
 * TestStateMachine.
 */
@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Test extends BaseAuditableEntity {

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private TestStatus status = TestStatus.DRAFT;

  @Column(name = "max_attempts")
  private Integer maxAttempts;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "available_from")
  private Instant availableFrom;

  @Column(name = "available_until")
  private Instant availableUntil;

  @Column(name = "shuffle_questions", nullable = false)
  @Builder.Default
  private boolean shuffleQuestions = false;

  @Column(name = "shuffle_options", nullable = false)
  @Builder.Default
  private boolean shuffleOptions = false;
}
