package de.codillas.enrollment.domain.model;

import de.codillas.shared.domain.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A cohort: a group of students running a {@code courseId} under a {@code teacherId}. */
@Entity
@Table(name = "study_groups")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Group extends BaseAuditableEntity {

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "start_date")
  private LocalDate startDate;
}
