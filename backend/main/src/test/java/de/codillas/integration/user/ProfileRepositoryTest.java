package de.codillas.integration.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import de.codillas.integration.BaseIntegrationTest;
import de.codillas.user.domain.model.Profile;
import de.codillas.user.domain.repository.ProfileRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfileRepository (integration)")
class ProfileRepositoryTest extends BaseIntegrationTest {

  @Autowired private ProfileRepository repository;

  @Test
  @DisplayName("findByUserId returns the saved profile with its audit timestamp")
  void findByUserId_returnsSavedProfile() {
    UUID userId = UUID.randomUUID();
    repository.save(Profile.builder().userId(userId).displayName("Ada").build());

    var found = repository.findByUserId(userId);

    assertThat(found).isPresent();
    assertThat(found.get().getDisplayName()).isEqualTo("Ada");
    assertThat(found.get().getCreatedAt()).isNotNull();
  }
}
