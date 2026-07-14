package de.codillas.homework.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The points a teacher awarded one rubric criterion on a {@link Grade}: the per-criterion
 * breakdown.
 */
@Entity
@Table(name = "grade_criteria")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GradeCriterion extends BaseAuditableEntity {

  @Column(name = "grade_id", nullable = false)
  private UUID gradeId;

  @Column(name = "criterion_id", nullable = false)
  private UUID criterionId;

  @Column(nullable = false)
  private int points;
}
