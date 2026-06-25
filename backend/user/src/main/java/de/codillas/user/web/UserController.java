package de.codillas.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.user.api.UserApi;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.service.UserService;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link UserApi}; "me" resolves the current user. */
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

  private final UserService service;

  @Override
  @RequiresAuthenticated
  public ResponseEntity<UserProfile> getMyProfile() {
    return ResponseEntity.ok(service.getMyProfile());
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<UserProfile> updateMyProfile(UpdateProfileRequest updateProfileRequest) {
    return ResponseEntity.ok(service.updateMyProfile(updateProfileRequest));
  }
}
