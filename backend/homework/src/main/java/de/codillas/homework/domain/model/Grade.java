package de.codillas.homework.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The grade a teacher gives a submission. One grade per submission. {@code score} is the raw score;
 * {@code effectiveScore} is what remains after the assignment's late penalty; {@code maxPoints} is
 * the denominator (100 for a flat grade, Σ criterion maxPoints for a rubric).
 */
@Entity
@Table(name = "grades", uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Grade extends BaseAuditableEntity {

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(nullable = false)
  private int score;

  @Column(name = "effective_score", nullable = false)
  private int effectiveScore;

  @Column(name = "max_points", nullable = false)
  @Builder.Default
  private int maxPoints = 100;

  @Column(name = "graded_by", nullable = false)
  private UUID gradedBy;
}
