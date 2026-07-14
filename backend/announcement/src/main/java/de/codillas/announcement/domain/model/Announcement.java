package de.codillas.announcement.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** One class-stream post in a group: authored by a teacher, read by the group's members. */
@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Announcement extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String body;

  @Column(nullable = false)
  @Builder.Default
  private boolean pinned = false;
}
