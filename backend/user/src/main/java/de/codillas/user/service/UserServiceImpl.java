package de.codillas.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.domain.model.Profile;
import de.codillas.user.domain.repository.ProfileRepository;
import de.codillas.user.mapper.ProfileMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private final ProfileRepository repository;
  private final ProfileMapper mapper;
  private final CurrentUser currentUser;

  @Override
  public UserProfile getMyProfile() {
    UUID userId = currentUser.id();
    Profile profile =
        repository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Profile", userId));
    return mapper.toResponse(profile);
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
