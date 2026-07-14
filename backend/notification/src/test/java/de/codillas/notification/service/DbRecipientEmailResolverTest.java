package de.codillas.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import de.codillas.notification.domain.model.RecipientEmail;
import de.codillas.notification.domain.repository.RecipientEmailRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DbRecipientEmailResolver — read model")
class DbRecipientEmailResolverTest {
  @Mock RecipientEmailRepository repository;
  @InjectMocks DbRecipientEmailResolver resolver;

  @Test
  @DisplayName("resolves a previously recorded email")
  void resolvesRecorded() {
    UUID userId = UUID.randomUUID();
    when(repository.findByUserId(userId))
        .thenReturn(Optional.of(RecipientEmail.builder().userId(userId).email("a@b.de").build()));
    assertThat(resolver.resolve(userId)).contains("a@b.de");
  }
}
