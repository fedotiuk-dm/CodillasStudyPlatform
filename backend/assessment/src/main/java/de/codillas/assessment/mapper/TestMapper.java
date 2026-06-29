package de.codillas.assessment.mapper;

import java.util.List;

import org.springframework.data.domain.Page;

import de.codillas.assessment.api.dto.CreateTestRequest;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.TestListResponse;
import de.codillas.assessment.api.dto.TestResponse;
import de.codillas.assessment.api.dto.TestSummary;
import de.codillas.assessment.domain.model.Test;
import de.codillas.shared.mapper.CentralMapperConfig;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = CentralMapperConfig.class)
public interface TestMapper {

  TestResponse toResponse(Test test, List<QuestionResponse> questions);

  TestSummary toSummary(Test test);

  TestListResponse toListResponse(Page<Test> page);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Test toEntity(CreateTestRequest request);
}
