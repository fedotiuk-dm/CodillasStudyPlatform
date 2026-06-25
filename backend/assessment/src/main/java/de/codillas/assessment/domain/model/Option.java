package de.codillas.assessment.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A selectable option of a choice question. {@code correct} is server-only — never exposed. */
@Entity
@Table(name = "options")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Option extends BaseAuditableEntity {

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(nullable = false, columnDefinition = "text")
  private String text;

  @Column(nullable = false)
  private boolean correct;

  @Column(nullable = false)
  private int position;
}
