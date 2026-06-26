package de.codillas.user.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.shared.security.CurrentUser;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.api.dto.UserProfileListResponse;
import de.codillas.user.domain.model.Profile;
import de.codillas.user.domain.repository.ProfileRepository;
import de.codillas.user.mapper.ProfileMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final ProfileRepository repository;
  private final ProfileMapper mapper;
  private final CurrentUser currentUser;

  /**
   * Returns the caller's profile, provisioning one on first access (i.e. right after a Keycloak
   * login) from the token's display name — so a freshly-created Keycloak user always has a profile.
   */
  @Override
  @Transactional
  public UserProfile getMyProfile() {
    UUID userId = currentUser.id();
    Profile profile = repository.findByUserId(userId).orElseGet(() -> provisionFromToken(userId));
    return mapper.toResponse(profile, currentUser.roles().stream().sorted().toList());
  }

  /** Creates and persists a profile on first login, seeding the display name from the JWT. */
  private Profile provisionFromToken(UUID userId) {
    Profile profile =
        repository.save(
            Profile.builder().userId(userId).displayName(currentUser.displayName()).build());
    log.info("Auto-created profile for user {} on first access", userId);
    return profile;
  }

  /** Lists local profiles, optionally filtered by a case-insensitive display-name substring. */
  @Override
  public UserProfileListResponse listProfiles(String q, Pageable pageable) {
    boolean hasQuery = q != null && !q.isBlank();
    return mapper.toListResponse(
        hasQuery
            ? repository.findByDisplayNameContainingIgnoreCase(q.strip(), pageable)
            : repository.findAll(pageable));
  }

  @Override
  @Transactional
  public UserProfile updateMyProfile(UpdateProfileRequest request) {
    UUID userId = currentUser.id();
    Profile profile =
        repository.findByUserId(userId).orElseGet(() -> Profile.builder().userId(userId).build());
    mapper.updateEntity(profile, request);
    return mapper.toResponse(repository.save(profile));
  }
}
