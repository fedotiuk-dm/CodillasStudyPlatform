package de.codillas.enrollment.mapper;

import java.util.List;

import org.springframework.data.domain.Page;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface GroupMapper {

  GroupResponse toResponse(Group entity);

  List<GroupResponse> toResponseList(List<Group> entities);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Group toEntity(CreateGroupRequest request);

  GroupListResponse toListResponse(Page<Group> page);

  /** Maps the generated DTO status enum to the domain enum (by name); null when no filter. */
  de.codillas.enrollment.domain.model.GroupStatus toDomainStatus(
      de.codillas.enrollment.api.dto.GroupStatus status);
}
