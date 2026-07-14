package de.codillas.homework.mapper;

import java.util.List;

import de.codillas.homework.api.dto.CriterionScoreResponse;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.domain.model.Grade;
import de.codillas.homework.domain.model.GradeCriterion;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface GradeMapper {

  @Mapping(target = "criterionScores", source = "criteria")
  GradeResponse toResponse(Grade entity, List<GradeCriterion> criteria);

  CriterionScoreResponse toCriterionScore(GradeCriterion criterion);

  List<CriterionScoreResponse> toCriterionScores(List<GradeCriterion> criteria);
}
