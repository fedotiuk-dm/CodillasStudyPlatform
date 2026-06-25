package de.codillas.enrollment.mapper;

import java.util.List;

import de.codillas.enrollment.api.dto.MembershipResponse;
import de.codillas.enrollment.domain.model.Membership;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface MembershipMapper {

  @Mapping(target = "enrolledAt", source = "createdAt")
  MembershipResponse toResponse(Membership entity);

  List<MembershipResponse> toResponseList(List<Membership> entities);
}
