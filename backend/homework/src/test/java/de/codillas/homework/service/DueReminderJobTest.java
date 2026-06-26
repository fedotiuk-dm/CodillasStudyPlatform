package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.AssignmentStatus;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.shared.event.AssignmentDueSoon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DueReminderJob")
class DueReminderJobTest {

  @Mock private AssignmentRepository repository;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private DueReminderJob job;

  @Test
  @DisplayName(
      "a PUBLISHED assignment due in 12h and not yet reminded → AssignmentDueSoon published and"
          + " flag set")
  void publishedDueSoon_publishesEventAndSetsFlag() {
    UUID assignmentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    Instant dueAt = Instant.now().plus(Duration.ofHours(12));
    Assignment assignment =
        Assignment.builder()
            .id(assignmentId)
            .groupId(groupId)
            .title("HW1")
            .status(AssignmentStatus.PUBLISHED)
            .dueAt(dueAt)
            .build();
    when(repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(
            eq(AssignmentStatus.PUBLISHED),
            any(Instant.class),
            any(Instant.class),
            any(Sort.class)))
        .thenReturn(List.of(assignment));

    job.remindDueSoon();

    verify(events).publishEvent(new AssignmentDueSoon(assignmentId, groupId, dueAt));
    assertThat(assignment.isDueReminderSent()).isTrue();
    verify(repository).save(assignment);
  }

  @Test
  @DisplayName("queries only PUBLISHED, not-yet-reminded assignments inside the 24h window")
  void queries24hWindowForPublishedUnreminded() {
    when(repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(any(), any(), any(), any()))
        .thenReturn(List.of());

    job.remindDueSoon();

    ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
    verify(repository)
        .findByStatusAndDueReminderSentFalseAndDueAtBetween(
            eq(AssignmentStatus.PUBLISHED),
            from.capture(),
            to.capture(),
            eq(AssignmentRepository.BY_DUE_AT));
    assertThat(Duration.between(from.getValue(), to.getValue())).isEqualTo(Duration.ofHours(24));
  }

  @Test
  @DisplayName(
      "nothing in the window (due in 5 days, already reminded, or not PUBLISHED) → no event, no"
          + " save")
  void noneInWindow_noEventNoSave() {
    when(repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(any(), any(), any(), any()))
        .thenReturn(List.of());

    job.remindDueSoon();

    verify(events, never()).publishEvent(any());
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("multiple due-soon assignments each get an event and a flag save")
  void multipleDueSoon_eachReminded() {
    Assignment a = published(Instant.now().plus(Duration.ofHours(3)));
    Assignment b = published(Instant.now().plus(Duration.ofHours(20)));
    when(repository.findByStatusAndDueReminderSentFalseAndDueAtBetween(any(), any(), any(), any()))
        .thenReturn(List.of(a, b));

    job.remindDueSoon();

    verify(events, times(2)).publishEvent(any(AssignmentDueSoon.class));
    verify(repository, times(2)).save(any(Assignment.class));
    assertThat(a.isDueReminderSent()).isTrue();
    assertThat(b.isDueReminderSent()).isTrue();
  }

  private static Assignment published(Instant dueAt) {
    return Assignment.builder()
        .id(UUID.randomUUID())
        .groupId(UUID.randomUUID())
        .title("HW")
        .status(AssignmentStatus.PUBLISHED)
        .dueAt(dueAt)
        .build();
  }
}
