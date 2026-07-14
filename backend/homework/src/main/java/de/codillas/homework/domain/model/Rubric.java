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

/** A grading rubric attached to one assignment; the grade is Σ of its criterion scores. */
@Entity
@Table(name = "rubrics")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Rubric extends BaseAuditableEntity {

  @Column(name = "assignment_id", nullable = false)
  private UUID assignmentId;
}
