package de.codillas.enrollment.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.enrollment.api.dto.EnrollStudentRequest;
import de.codillas.enrollment.api.dto.MembershipResponse;
import de.codillas.enrollment.domain.model.Membership;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface MembershipMapper {

  @Mapping(target = "enrolledAt", source = "createdAt")
  MembershipResponse toResponse(Membership entity);

  List<MembershipResponse> toResponseList(List<Membership> entities);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Membership toEntity(EnrollStudentRequest request, UUID groupId);
}
