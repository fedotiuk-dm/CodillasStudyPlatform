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

/** A teacher's textual feedback on a submission. May exist without a grade (e.g. "rework this"). */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Review extends BaseAuditableEntity {

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "reviewer_id", nullable = false)
  private UUID reviewerId;

  @Column(nullable = false, columnDefinition = "text")
  private String comment;
}
