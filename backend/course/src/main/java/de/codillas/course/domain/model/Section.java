package de.codillas.course.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;
import de.codillas.shared.domain.Sortable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A section (chapter/module) within a course. Ordered via {@link Sortable}; groups lessons. */
@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Section extends BaseAuditableEntity implements Sortable {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
