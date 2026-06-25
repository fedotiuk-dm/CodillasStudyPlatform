package de.codillas.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import de.codillas.shared.security.CurrentUser;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.api.dto.UserProfileListResponse;
import de.codillas.user.domain.model.Profile;
import de.codillas.user.domain.repository.ProfileRepository;
import de.codillas.user.mapper.ProfileMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  @DisplayName("getMyProfile provisions a profile from the token on first access")
  void getMyProfile_whenMissing_provisionsFromToken() {
    UUID userId = UUID.randomUUID();
    UserProfile dto = new UserProfile(userId, "Ada Admin");
    when(currentUser.id()).thenReturn(userId);
    when(currentUser.displayName()).thenReturn("Ada Admin");
    when(repository.findByUserId(userId)).thenReturn(Optional.empty());
    when(repository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));
    when(mapper.toResponse(any(Profile.class))).thenReturn(dto);

    assertThat(service.getMyProfile()).isSameAs(dto);

    ArgumentCaptor<Profile> saved = ArgumentCaptor.forClass(Profile.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getUserId()).isEqualTo(userId);
    assertThat(saved.getValue().getDisplayName()).isEqualTo("Ada Admin");
  }

  @Test
  @DisplayName("listProfiles without a query lists all profiles")
  void listProfiles_withoutQuery_listsAll() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Profile> page = new PageImpl<>(List.of());
    UserProfileListResponse dto = new UserProfileListResponse().content(List.of());
    when(repository.findAll(pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(dto);

    assertThat(service.listProfiles("  ", pageable)).isSameAs(dto);
  }

  @Test
  @DisplayName("listProfiles with a query filters by display name, case-insensitive")
  void listProfiles_withQuery_filtersByName() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Profile> page = new PageImpl<>(List.of());
    UserProfileListResponse dto = new UserProfileListResponse().content(List.of());
    when(repository.findByDisplayNameContainingIgnoreCase("ada", pageable)).thenReturn(page);
    when(mapper.toListResponse(page)).thenReturn(dto);

    assertThat(service.listProfiles("ada", pageable)).isSameAs(dto);
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
