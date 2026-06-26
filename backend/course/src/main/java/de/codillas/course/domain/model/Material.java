package de.codillas.course.domain.model;

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

/**
 * A lesson material. {@code FILE} carries a {@code fileId} into the files module; {@code LINK}
 * carries a {@code url}. Which field is required is enforced in the service, not the schema.
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Material extends BaseAuditableEntity implements Sortable {

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private MaterialType type;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String url;

  @Column(name = "file_id")
  private UUID fileId;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
