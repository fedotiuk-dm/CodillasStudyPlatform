package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.repository.AssignmentRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentServiceImpl — GroupArchived")
class AssignmentServiceArchiveTest {

  @Mock private AssignmentRepository repository;
  @InjectMocks private AssignmentServiceImpl service;

  @Test
  @DisplayName("mutes deadline reminders for a finished cohort, skipping the already-sent ones")
  void mutesPendingReminders() {
    UUID groupId = UUID.randomUUID();
    Assignment pending = Assignment.builder().dueReminderSent(false).build();
    Assignment alreadySent = Assignment.builder().dueReminderSent(true).build();
    when(repository.findByGroupId(groupId)).thenReturn(List.of(pending, alreadySent));

    service.onGroupArchived(groupId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Assignment>> saved = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(saved.capture());
    assertThat(saved.getValue()).containsExactly(pending);
    assertThat(pending.isDueReminderSent()).isTrue();
  }
}
