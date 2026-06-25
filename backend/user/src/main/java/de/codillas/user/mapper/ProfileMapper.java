package de.codillas.user.mapper;

import de.codillas.shared.mapper.CentralMapperConfig;
import de.codillas.user.api.dto.UpdateProfileRequest;
import de.codillas.user.api.dto.UserProfile;
import de.codillas.user.domain.model.Profile;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = CentralMapperConfig.class)
public interface ProfileMapper {

  UserProfile toResponse(Profile entity);

  @BeanMapping(
      ignoreByDefault = true,
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "displayName")
  @Mapping(target = "bio")
  void updateEntity(@MappingTarget Profile entity, UpdateProfileRequest request);
}
