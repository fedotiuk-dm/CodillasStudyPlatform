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

/** One ordered criterion of a {@link Rubric}: a label and the maximum points it can award. */
@Entity
@Table(name = "rubric_criteria")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RubricCriterion extends BaseAuditableEntity {

  @Column(name = "rubric_id", nullable = false)
  private UUID rubricId;

  @Column(nullable = false, length = 200)
  private String label;

  @Column(name = "max_points", nullable = false)
  private int maxPoints;

  @Column(nullable = false)
  private int position;
}
