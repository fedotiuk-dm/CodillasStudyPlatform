package de.codillas.enrollment.mapper;

import de.codillas.enrollment.api.dto.CreateGroupRequest;
import de.codillas.enrollment.api.dto.GroupListResponse;
import de.codillas.enrollment.api.dto.GroupResponse;
import de.codillas.enrollment.domain.model.Group;
import de.codillas.shared.mapper.CentralMapperConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(config = CentralMapperConfig.class)
public interface GroupMapper {

  GroupResponse toResponse(Group entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Group toEntity(CreateGroupRequest request);

  GroupListResponse toListResponse(Page<Group> page);
}
