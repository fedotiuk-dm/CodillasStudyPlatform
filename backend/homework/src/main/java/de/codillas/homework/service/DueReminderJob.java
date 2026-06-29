package de.codillas.homework.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
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
  private final ApplicationEventPublisher events;
  private final Duration window;

  public DueReminderJob(
      AssignmentRepository repository,
      ApplicationEventPublisher events,
      @Value("${homework.due-reminder.window:PT24H}") Duration window) {
    this.repository = repository;
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
    for (Assignment assignment : dueSoon) {
      events.publishEvent(
          new AssignmentDueSoon(
              assignment.getId(), assignment.getGroupId(), assignment.getDueAt()));
      assignment.setDueReminderSent(true);
      repository.save(assignment);
    }
  }
}
