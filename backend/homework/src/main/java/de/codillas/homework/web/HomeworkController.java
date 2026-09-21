package de.codillas.homework.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import de.codillas.homework.api.HomeworkApi;
import de.codillas.homework.api.dto.AssignmentListResponse;
import de.codillas.homework.api.dto.AssignmentResponse;
import de.codillas.homework.api.dto.CreateAssignmentRequest;
import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.CreateRubricRequest;
import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.api.dto.RubricResponse;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.api.dto.UpdateSubmissionRequest;
import de.codillas.homework.service.AssignmentService;
import de.codillas.homework.service.RubricService;
import de.codillas.homework.service.SubmissionService;
import de.codillas.shared.security.RequiresAuthenticated;
import de.codillas.shared.security.RequiresStudent;
import de.codillas.shared.security.RequiresTeacher;

import lombok.RequiredArgsConstructor;

/** Thin delegator — implements the generated {@link HomeworkApi}. */
@RestController
@RequiredArgsConstructor
public class HomeworkController implements HomeworkApi {

  private final AssignmentService assignmentService;
  private final SubmissionService submissionService;
  private final RubricService rubricService;

  @Override
  @RequiresTeacher
  public ResponseEntity<AssignmentResponse> createAssignment(
      CreateAssignmentRequest createAssignmentRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(assignmentService.createAssignment(createAssignmentRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<AssignmentResponse> publishAssignment(UUID assignmentId) {
    return ResponseEntity.ok(assignmentService.publishAssignment(assignmentId));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<AssignmentListResponse> listAssignments(UUID groupId, Pageable pageable) {
    return ResponseEntity.ok(assignmentService.listAssignments(groupId, pageable));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<SubmissionResponse> createSubmission(
      UUID assignmentId, CreateSubmissionRequest createSubmissionRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(submissionService.createSubmission(assignmentId, createSubmissionRequest));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<SubmissionResponse> updateSubmission(
      UUID submissionId, UpdateSubmissionRequest updateSubmissionRequest) {
    return ResponseEntity.ok(
        submissionService.updateSubmission(submissionId, updateSubmissionRequest));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<SubmissionResponse> submitSubmission(UUID submissionId) {
    return ResponseEntity.ok(submissionService.submitSubmission(submissionId));
  }

  @Override
  @RequiresStudent
  public ResponseEntity<List<SubmissionResponse>> listSubmissions(UUID assignmentId) {
    return ResponseEntity.ok(submissionService.listSubmissions(assignmentId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<ReviewResponse> reviewSubmission(
      UUID submissionId, CreateReviewRequest createReviewRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(submissionService.reviewSubmission(submissionId, createReviewRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<GradeResponse> gradeSubmission(
      UUID submissionId, CreateGradeRequest createGradeRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(submissionService.gradeSubmission(submissionId, createGradeRequest));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<SubmissionResponse> returnSubmission(UUID submissionId) {
    return ResponseEntity.ok(submissionService.returnSubmission(submissionId));
  }

  @Override
  @RequiresTeacher
  public ResponseEntity<RubricResponse> createRubric(
      UUID assignmentId, CreateRubricRequest createRubricRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(rubricService.createRubric(assignmentId, createRubricRequest));
  }

  @Override
  @RequiresAuthenticated
  public ResponseEntity<RubricResponse> getRubric(UUID assignmentId) {
    return ResponseEntity.ok(rubricService.getRubric(assignmentId));
  }
}
