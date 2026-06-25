package de.codillas.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.domain.model.Profile;
import de.codillas.user.domain.repository.ProfileRepository;
import de.codillas.user.mapper.ProfileMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

  @Mock private ProfileRepository repository;
  @Mock private ProfileMapper mapper;
  @Mock private CurrentUser currentUser;
  @InjectMocks private UserServiceImpl service;

  @Test
  @DisplayName("getMyProfile returns the current user's mapped profile")
  void getMyProfile_returnsMappedProfile() {
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder().userId(userId).displayName("Ada").build();
    UserProfile dto = new UserProfile(userId, "Ada");
    when(currentUser.id()).thenReturn(userId);
    when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));
    when(mapper.toResponse(profile)).thenReturn(dto);

    assertThat(service.getMyProfile()).isSameAs(dto);
  }

  @Test
  @DisplayName("getMyProfile throws NotFound when the profile does not exist")
  void getMyProfile_whenMissing_throwsNotFound() {
    UUID userId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(userId);
    when(repository.findByUserId(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(service::getMyProfile).isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("updateMyProfile upserts the profile and returns it")
  void updateMyProfile_upsertsAndReturns() {
    UUID userId = UUID.randomUUID();
    Profile profile = Profile.builder().userId(userId).build();
    UpdateProfileRequest req = new UpdateProfileRequest("Ada");
    UserProfile dto = new UserProfile(userId, "Ada");
    when(currentUser.id()).thenReturn(userId);
    when(repository.findByUserId(userId)).thenReturn(Optional.of(profile));
    when(repository.save(profile)).thenReturn(profile);
    when(mapper.toResponse(profile)).thenReturn(dto);

    assertThat(service.updateMyProfile(req)).isSameAs(dto);
    verify(mapper).updateEntity(profile, req);
  }
}
