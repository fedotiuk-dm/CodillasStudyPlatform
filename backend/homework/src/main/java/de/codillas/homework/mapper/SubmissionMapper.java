package de.codillas.homework.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.domain.model.Submission;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface SubmissionMapper {

  SubmissionResponse toResponse(Submission entity);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Submission toEntity(
      CreateSubmissionRequest request, UUID assignmentId, UUID studentId, int version);

  List<SubmissionResponse> toResponseList(List<Submission> entities);
}
