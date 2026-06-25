package de.codillas.assessment.domain.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A student's answer to one question within an attempt. Unique per (attempt, question). Choice
 * answers fill {@code selectedOptionIds}; SHORT_TEXT fills {@code text}. {@code awardedPoints} is
 * null until graded.
 */
@Entity
@Table(
    name = "answers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Answer extends BaseAuditableEntity {

  @Column(name = "attempt_id", nullable = false)
  private UUID attemptId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @ElementCollection
  @CollectionTable(name = "answer_selected_options", joinColumns = @JoinColumn(name = "answer_id"))
  @Column(name = "option_id", nullable = false)
  @Builder.Default
  private Set<UUID> selectedOptionIds = new HashSet<>();

  @Column(columnDefinition = "text")
  private String text;

  @Column(name = "awarded_points")
  private Integer awardedPoints;
}
