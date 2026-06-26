package de.codillas.enrollment.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import de.codillas.shared.domain.BaseAuditableEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A lesson session a group runs at a point in time (Google Meet link + optional recording). */
@Entity
@Table(name = "scheduled_lessons")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ScheduledLesson extends BaseAuditableEntity {

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  /**
   * Course lesson this session runs, by id (the course module owns it). Optional for ad-hoc
   * sessions.
   */
  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "meet_link")
  private String meetLink;

  @Column(name = "recording_url")
  private String recordingUrl;
}
