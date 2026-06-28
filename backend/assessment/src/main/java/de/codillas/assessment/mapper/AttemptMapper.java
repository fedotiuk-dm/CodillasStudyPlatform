package de.codillas.assessment.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.codillas.assessment.api.dto.AnswerResponse;
import de.codillas.assessment.api.dto.AttemptResponse;
import de.codillas.assessment.api.dto.SaveAnswerRequest;
import de.codillas.assessment.domain.model.Answer;
import de.codillas.assessment.domain.model.Attempt;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface AttemptMapper {

  @Mapping(target = "answers", source = "answers")
  AttemptResponse toResponse(Attempt attempt, List<AnswerResponse> answers);

  AnswerResponse toAnswerResponse(Answer answer);

  List<AnswerResponse> toAnswerResponses(List<Answer> answers);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Attempt toEntity(UUID testId, UUID studentId, int attemptNumber, Instant startedAt);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Answer toAnswer(SaveAnswerRequest request, UUID attemptId);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(
      target = "selectedOptionIds",
      source = "selectedOptionIds",
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
  @Mapping(target = "text", source = "text")
  void updateAnswer(@MappingTarget Answer answer, SaveAnswerRequest request);
}
