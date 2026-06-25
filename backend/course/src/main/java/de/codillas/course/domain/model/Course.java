package de.codillas.course.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "courses")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Course extends BaseAuditableEntity {

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "text")
  private String description;
}
