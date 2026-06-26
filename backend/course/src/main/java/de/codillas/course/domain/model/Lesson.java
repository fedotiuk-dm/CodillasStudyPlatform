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

/**
 * A lesson within a section. Carries the live-session Meet link and post-session recording URL; its
 * materials live in the Material table. Homework and tests reference a lesson by id.
 */
@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Lesson extends BaseAuditableEntity implements Sortable {

  @Column(name = "section_id", nullable = false)
  private UUID sectionId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String summary;

  @Column(name = "meeting_url", columnDefinition = "text")
  private String meetingUrl;

  @Column(name = "recording_url", columnDefinition = "text")
  private String recordingUrl;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;
}
