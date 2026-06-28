package de.codillas.homework.mapper;

import java.util.List;
import java.util.UUID;

import de.codillas.homework.api.dto.CreateRubricCriterionRequest;
import de.codillas.homework.api.dto.RubricCriterionResponse;
import de.codillas.homework.api.dto.RubricResponse;
import de.codillas.homework.domain.model.Rubric;
import de.codillas.homework.domain.model.RubricCriterion;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface RubricMapper {

  RubricCriterionResponse toCriterionResponse(RubricCriterion criterion);

  List<RubricCriterionResponse> toCriterionResponses(List<RubricCriterion> criteria);

  RubricResponse toResponse(Rubric rubric, List<RubricCriterionResponse> criteria);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  @Mapping(target = "rubricId", source = "rubricId")
  @Mapping(target = "position", source = "position")
  RubricCriterion toCriterion(CreateRubricCriterionRequest request, UUID rubricId, int position);
}
