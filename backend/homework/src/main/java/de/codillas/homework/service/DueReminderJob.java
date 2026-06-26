package de.codillas.homework.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.shared.event.AssignmentDueSoon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hourly job that reminds group members when a published assignment's deadline is within the next
 * 24h. Publishes {@link AssignmentDueSoon} once per assignment (guarded by {@code
 * dueReminderSent}).
 *
 * <p>Disabled under the {@code integration-test} profile so the scheduler never fires during the
 * deterministic IT suite.
 */
@Component
@Profile("!integration-test")
@RequiredArgsConstructor
@Slf4j
public class DueReminderJob {

  private static final Duration WINDOW = Duration.ofHours(24);

  private final AssignmentRepository repository;
  private final ApplicationEventPublisher events;

  /** Top of every hour: fan out reminders for assignments due within the next 24h. */
  @Scheduled(cron = "0 0 * * * *")
  @Transactional
  public void remindDueSoon() {
    Instant now = Instant.now();
    List<Assignment> dueSoon =
        repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(
            AssignmentStatus.PUBLISHED, now, now.plus(WINDOW), AssignmentRepository.BY_DUE_AT);
    log.debug("Found {} assignment(s) due within the next 24h", dueSoon.size());
    for (Assignment assignment : dueSoon) {
      events.publishEvent(
          new AssignmentDueSoon(
              assignment.getId(), assignment.getGroupId(), assignment.getDueAt()));
      assignment.setDueReminderSent(true);
      repository.save(assignment);
    }
  }
}
