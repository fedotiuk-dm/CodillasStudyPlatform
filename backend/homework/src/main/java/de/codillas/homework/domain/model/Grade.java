package de.codillas.homework.domain.model;

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

/** The numeric grade (0–100) a teacher gives a submission. One grade per submission. */
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

  @Column(name = "graded_by", nullable = false)
  private UUID gradedBy;
}
