package de.codillas.user.mapper;

import java.util.List;

import org.springframework.data.domain.Page;

import de.codillas.shared.mapper.CentralMapperConfig;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.api.dto.UserProfileListResponse;
import de.codillas.user.domain.model.Profile;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = CentralMapperConfig.class)
public interface ProfileMapper {

  /** List / generic mapping — roles are a per-request concern, not stored on the profile. */
  @Mapping(target = "roles", ignore = true)
  UserProfile toResponse(Profile entity);

  /** {@code /me} mapping — surfaces the caller's granted roles alongside their profile. */
  UserProfile toResponse(Profile entity, List<String> roles);

  UserProfileListResponse toListResponse(Page<Profile> page);

  @BeanMapping(
      ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "displayName")
  @Mapping(target = "bio")
  void updateEntity(@MappingTarget Profile entity, UpdateProfileRequest request);
}
