package de.codillas.user.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Display profile for a Keycloak-backed user, keyed by the Keycloak subject ({@code userId}). */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Profile extends BaseAuditableEntity {

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(length = 500)
  private String bio;
}
