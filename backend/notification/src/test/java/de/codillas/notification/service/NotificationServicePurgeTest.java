package de.codillas.notification.service;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import de.codillas.notification.domain.repository.NotificationMembershipRepository;
import de.codillas.notification.domain.repository.NotificationRepository;
import de.codillas.notification.mapper.NotificationMapper;
import de.codillas.shared.event.GroupDeleted;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService — purge on GroupDeleted")
class NotificationServicePurgeTest {
  @Mock NotificationRepository repository;
  @Mock NotificationMembershipRepository membershipRepository;
  @Mock NotificationMapper mapper;
  @Mock EmailNotifier emailNotifier;
  @InjectMocks NotificationServiceImpl service;

  @Test
  @DisplayName("deletes the group's membership rows")
  void purges() {
    UUID groupId = UUID.randomUUID();
    service.onGroupDeleted(new GroupDeleted(groupId));
    verify(membershipRepository).deleteByGroupId(groupId);
  }
}
