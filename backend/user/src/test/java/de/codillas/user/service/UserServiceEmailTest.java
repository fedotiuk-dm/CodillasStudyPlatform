package de.codillas.user.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.shared.event.UserEmailChanged;
import de.codillas.shared.security.CurrentUser;
import de.codillas.user.domain.repository.ProfileRepository;
import de.codillas.user.mapper.ProfileMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl — capture email at provisioning")
class UserServiceEmailTest {
  @Mock ProfileRepository repository;
  @Mock ProfileMapper mapper;
  @Mock CurrentUser currentUser;
  @Mock ApplicationEventPublisher events;
  @InjectMocks UserServiceImpl service;

  @Test
  @DisplayName("provisioning a new profile publishes UserEmailChanged")
  void publishesOnProvision() {
    UUID userId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(userId);
    when(currentUser.displayName()).thenReturn("Stu");
    when(currentUser.email()).thenReturn("stu@codillas.de");
    when(repository.findByUserId(userId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.roles()).thenReturn(Set.of("STUDENT"));
    when(mapper.toResponse(any(), any())).thenReturn(null);

    service.getMyProfile();

    verify(events).publishEvent(new UserEmailChanged(userId, "stu@codillas.de"));
  }

  @Test
  @DisplayName("does not publish when the token carries no email")
  void noEmailNoPublish() {
    UUID userId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(userId);
    when(currentUser.displayName()).thenReturn("Stu");
    when(currentUser.email()).thenReturn(null);
    when(repository.findByUserId(userId)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.roles()).thenReturn(Set.of("STUDENT"));
    when(mapper.toResponse(any(), any())).thenReturn(null);

    service.getMyProfile();

    verify(events, org.mockito.Mockito.never()).publishEvent(any(UserEmailChanged.class));
  }
}
