package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import de.codillas.homework.domain.model.ArchivedGroup;
import de.codillas.homework.domain.repository.ArchivedGroupRepository;
import de.codillas.homework.domain.repository.AssignmentRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentServiceImpl — cohort archive/resume read model")
class AssignmentServiceArchiveTest {

  @Mock private AssignmentRepository repository;
  @Mock private ArchivedGroupRepository archivedGroups;
  @InjectMocks private AssignmentServiceImpl service;

  @Test
  @DisplayName("archiving records the cohort so the reminder job can skip it")
  void archiveRecordsGroup() {
    UUID groupId = UUID.randomUUID();
    when(archivedGroups.existsByGroupId(groupId)).thenReturn(false);

    service.onGroupArchived(groupId);

    ArgumentCaptor<ArchivedGroup> saved = ArgumentCaptor.forClass(ArchivedGroup.class);
    verify(archivedGroups).save(saved.capture());
    assertThat(saved.getValue().getGroupId()).isEqualTo(groupId);
  }

  @Test
  @DisplayName("archiving twice is a no-op — delivery is at-least-once")
  void archiveIsIdempotent() {
    UUID groupId = UUID.randomUUID();
    when(archivedGroups.existsByGroupId(groupId)).thenReturn(true);

    service.onGroupArchived(groupId);

    verify(archivedGroups, never()).save(any());
  }

  @Test
  @DisplayName("resuming drops the record so reminders flow again")
  void resumeClearsGroup() {
    UUID groupId = UUID.randomUUID();

    service.onGroupResumed(groupId);

    verify(archivedGroups).deleteByGroupId(groupId);
  }
}
