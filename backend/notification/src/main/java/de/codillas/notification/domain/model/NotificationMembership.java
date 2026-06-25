package de.codillas.notification.domain.model;

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

/** Local group roster (fed by StudentEnrolled) so AssignmentPublished can fan out to members. */
@Entity
@Table(
    name = "notification_memberships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class NotificationMembership extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;
}
