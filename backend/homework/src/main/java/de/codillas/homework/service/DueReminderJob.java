package de.codillas.homework.service;

import static java.util.stream.Collectors.toSet;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.domain.model.ArchivedGroup;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.repository.ArchivedGroupRepository;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.shared.event.AssignmentDueSoon;

import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job that reminds group members when a published assignment's deadline falls inside the
 * reminder window. Publishes {@link AssignmentDueSoon} once per assignment (guarded by {@code
 * dueReminderSent}). Both the schedule ({@code homework.due-reminder.cron}) and the look-ahead
 * window ({@code homework.due-reminder.window}) are configurable — defaults are hourly / 24h.
 *
 * <p>Disabled under the {@code integration-test} profile so the scheduler never fires during the
 * deterministic IT suite.
 */
@Component
@Profile("!integration-test")
@Slf4j
public class DueReminderJob {

  private final AssignmentRepository repository;
  private final ArchivedGroupRepository archivedGroups;
  private final ApplicationEventPublisher events;
  private final Duration window;

  public DueReminderJob(
      AssignmentRepository repository,
      ArchivedGroupRepository archivedGroups,
      ApplicationEventPublisher events,
      @Value("${homework.due-reminder.window:PT24H}") Duration window) {
    this.repository = repository;
    this.archivedGroups = archivedGroups;
    this.events = events;
    this.window = window;
  }

  /** Fan out reminders for assignments due within the configured look-ahead window. */
  @Scheduled(cron = "${homework.due-reminder.cron:0 0 * * * *}")
  @Transactional
  public void remindDueSoon() {
    Instant now = Instant.now();
    List<Assignment> dueSoon =
        repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(
            AssignmentStatus.PUBLISHED, now, now.plus(window), AssignmentRepository.BY_DUE_AT);
    log.debug("Found {} assignment(s) due within {}", dueSoon.size(), window);
    Set<UUID> archived = archivedGroupIdsAmong(dueSoon);
    for (Assignment assignment : dueSoon) {
      if (archived.contains(assignment.getGroupId())) {
        continue; // retired cohort — nobody is still working on this
      }
      events.publishEvent(
          new AssignmentDueSoon(
              assignment.getId(), assignment.getGroupId(), assignment.getDueAt()));
      assignment.setDueReminderSent(true);
      repository.save(assignment);
    }
  }

  /** One lookup per run rather than one per assignment. */
  private Set<UUID> archivedGroupIdsAmong(List<Assignment> assignments) {
    Set<UUID> groupIds = assignments.stream().map(Assignment::getGroupId).collect(toSet());
    return groupIds.isEmpty()
        ? Set.of()
        : archivedGroups.findByGroupIdIn(groupIds).stream()
            .map(ArchivedGroup::getGroupId)
            .collect(toSet());
  }
}
