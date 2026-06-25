package de.codillas.assessment.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;
import de.codillas.shared.domain.Sortable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A question within a test. Ordered via {@link Sortable}; its options live in the Option table. */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Question extends BaseAuditableEntity implements Sortable {

  @Column(name = "test_id", nullable = false)
  private UUID testId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QuestionType type;

  @Column(nullable = false, columnDefinition = "text")
  private String prompt;

  @Column(nullable = false)
  private int points;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
