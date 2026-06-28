package de.codillas.homework.service;

import java.util.List;
import java.util.UUID;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.api.dto.UpdateSubmissionRequest;

public interface SubmissionService {

  SubmissionResponse createSubmission(UUID assignmentId, CreateSubmissionRequest request);

  SubmissionResponse updateSubmission(UUID submissionId, UpdateSubmissionRequest request);

  SubmissionResponse submitSubmission(UUID submissionId);

  List<SubmissionResponse> listSubmissions(UUID assignmentId);

  ReviewResponse reviewSubmission(UUID submissionId, CreateReviewRequest request);

  GradeResponse gradeSubmission(UUID submissionId, CreateGradeRequest request);

  SubmissionResponse returnSubmission(UUID submissionId);

  /** Delete the group's assignments and their submissions/reviews/grades (fed by GroupDeleted). */
  void onGroupDeleted(UUID groupId);
}
