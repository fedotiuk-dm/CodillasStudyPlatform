package de.codillas.gradebook.domain.model;

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

/** Read-model copy of group membership (fed by StudentEnrolled) so a group's roster is local. */
@Entity
@Table(
    name = "gradebook_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GradebookMembership extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;
}
