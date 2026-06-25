package de.codillas.assessment.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.assessment.api.dto.CreateOptionRequest;
import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.OptionResponse;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface QuestionMapper {

  @Mapping(target = "options", source = "options")
  QuestionResponse toResponse(Question question, List<OptionResponse> options);

  /** Correctness is intentionally not exposed. */
  OptionResponse toOptionResponse(Option option);

  List<OptionResponse> toOptionResponses(List<Option> options);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Question toEntity(CreateQuestionRequest request, UUID testId, int sortOrder);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Option toOption(CreateOptionRequest request, UUID questionId, int position);
}
