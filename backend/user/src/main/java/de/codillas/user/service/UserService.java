package de.codillas.user.service;

import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;

public interface UserService {

  UserProfile getMyProfile();

  UserProfile updateMyProfile(UpdateProfileRequest request);
}
