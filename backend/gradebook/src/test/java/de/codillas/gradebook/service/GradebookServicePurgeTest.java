package de.codillas.gradebook.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import de.codillas.gradebook.domain.repository.GradebookMembershipRepository;
import de.codillas.gradebook.domain.repository.ProgressEntryRepository;
import de.codillas.gradebook.mapper.GradebookMapper;
import de.codillas.shared.event.GroupDeleted;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradebookService — purge on GroupDeleted")
class GradebookServicePurgeTest {
  @Mock ProgressEntryRepository repository;
  @Mock GradebookMembershipRepository membershipRepository;
  @Mock GradebookMapper mapper;
  @Mock CurrentUser currentUser;
  @InjectMocks GradebookServiceImpl service;

  @Test
  @DisplayName("deletes the group's membership rows")
  void purges() {
    UUID groupId = UUID.randomUUID();
    service.purgeGroup(new GroupDeleted(groupId));
    verify(membershipRepository).deleteByGroupId(groupId);
  }
}
