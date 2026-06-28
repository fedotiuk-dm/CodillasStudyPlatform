package de.codillas.gradebook.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A derived, event-fed grade record. Unique per (source, sourceId) so a re-grade updates in place
 * rather than duplicating. {@code recordedAt} is the auditable {@code createdAt}.
 */
@Entity
@Table(
    name = "progress_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"source", "source_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProgressEntry extends BaseAuditableEntity {

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GradeSource source;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(name = "reference_id", nullable = false)
  private UUID referenceId;

  @Column(nullable = false)
  private int score;

  @Column(name = "max_points", nullable = false)
  @Builder.Default
  private int maxPoints = 100;

  @Column(name = "group_id")
  private UUID groupId;
}
