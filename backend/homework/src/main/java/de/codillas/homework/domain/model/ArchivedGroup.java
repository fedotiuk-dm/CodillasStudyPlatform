package de.codillas.homework.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Local read model of the cohorts enrollment has retired. homework may not read enrollment, and the
 * due-reminder job needs the answer on every run — including for assignments authored after the
 * cohort was archived, which is why this is keyed by group rather than flagged per assignment.
 */
@Entity
@Table(name = "archived_groups")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ArchivedGroup extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false, unique = true)
  private UUID groupId;
}
