package de.codillas.homework.mapper;

import org.springframework.data.domain.Page;

import de.codillas.homework.api.dto.AssignmentListResponse;
import de.codillas.homework.api.dto.AssignmentResponse;
import de.codillas.homework.api.dto.CreateAssignmentRequest;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface AssignmentMapper {

  AssignmentResponse toResponse(Assignment entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Assignment toEntity(CreateAssignmentRequest request);

  AssignmentListResponse toListResponse(Page<Assignment> page);
}
