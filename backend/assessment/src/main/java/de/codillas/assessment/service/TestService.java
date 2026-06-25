package de.codillas.assessment.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.CreateTestRequest;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.TestListResponse;
import de.codillas.assessment.api.dto.TestResponse;

public interface TestService {

  TestResponse createTest(CreateTestRequest request);

  TestResponse publishTest(UUID testId);

  QuestionResponse addQuestion(UUID testId, CreateQuestionRequest request);

  TestResponse getTest(UUID testId);

  TestListResponse listTests(UUID lessonId, Pageable pageable);
}
