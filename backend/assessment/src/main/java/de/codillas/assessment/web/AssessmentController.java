package de.codillas.assessment.web;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.assessment.api.AssessmentApi;
import de.codillas.assessment.api.dto.AnswerResponse;
import de.codillas.assessment.api.dto.AttemptResponse;
import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.CreateTestRequest;
import de.codillas.assessment.api.dto.GradeAnswerRequest;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.SaveAnswerRequest;
import de.codillas.assessment.api.dto.TestListResponse;
import de.codillas.assessment.api.dto.TestResponse;
import de.codillas.assessment.service.AttemptService;
import de.codillas.assessment.service.TestService;
import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.shared.security.RequiresStudent;
import de.codillas.shared.security.RequiresTeacher;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link AssessmentApi}. */
@RestController
@RequiredArgsConstructor
public class AssessmentController implements AssessmentApi {

  private final TestService testService;
  private final AttemptService attemptService;

  @Override
  @RequiresTeacher
  public ResponseEntity<TestResponse> createTest(CreateTestRequest createTestRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(testService.createTest(createTestRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<TestResponse> publishTest(UUID testId) {
    return ResponseEntity.ok(testService.publishTest(testId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<QuestionResponse> addQuestion(
      UUID testId, CreateQuestionRequest createQuestionRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(testService.addQuestion(testId, createQuestionRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<TestResponse> getTest(UUID testId) {
    return ResponseEntity.ok(testService.getTest(testId));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<TestListResponse> listTests(UUID lessonId, Pageable pageable) {
    return ResponseEntity.ok(testService.listTests(lessonId, pageable));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<AttemptResponse> startAttempt(UUID testId) {
    return ResponseEntity.status(HttpStatus.CREATED).body(attemptService.startAttempt(testId));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<AnswerResponse> saveAnswer(
      UUID attemptId, SaveAnswerRequest saveAnswerRequest) {
    return ResponseEntity.ok(attemptService.saveAnswer(attemptId, saveAnswerRequest));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<AttemptResponse> submitAttempt(UUID attemptId) {
    return ResponseEntity.ok(attemptService.submitAttempt(attemptId));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<AttemptResponse> getAttempt(UUID attemptId) {
    return ResponseEntity.ok(attemptService.getAttempt(attemptId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<AnswerResponse> gradeAnswer(
      UUID attemptId, UUID answerId, GradeAnswerRequest gradeAnswerRequest) {
    return ResponseEntity.ok(attemptService.gradeAnswer(attemptId, answerId, gradeAnswerRequest));
  }
}
