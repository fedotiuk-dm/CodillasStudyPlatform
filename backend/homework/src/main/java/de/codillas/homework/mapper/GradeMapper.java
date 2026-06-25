package de.codillas.homework.mapper;

import java.util.UUID;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.domain.model.Grade;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface GradeMapper {

  GradeResponse toResponse(Grade entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Grade toEntity(CreateGradeRequest request, UUID submissionId, UUID gradedBy);
}
