package de.codillas.user.service;

import org.springframework.data.domain.Pageable;

import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.api.dto.UserProfileListResponse;

public interface UserService {

  UserProfile getMyProfile();

  UserProfile updateMyProfile(UpdateProfileRequest request);

  UserProfileListResponse listProfiles(String q, Pageable pageable);
}
