package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.domain.SubmissionStateMachine;
import de.codillas.homework.domain.model.Grade;
import de.codillas.homework.domain.model.Review;
import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.model.SubmissionStatus;
import de.codillas.homework.domain.repository.GradeRepository;
import de.codillas.homework.domain.repository.ReviewRepository;
import de.codillas.homework.domain.repository.SubmissionRepository;
import de.codillas.homework.mapper.GradeMapper;
import de.codillas.homework.mapper.ReviewMapper;
import de.codillas.homework.mapper.SubmissionMapper;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionService")
class SubmissionServiceTest {

  @Mock private SubmissionRepository repository;
  @Mock private ReviewRepository reviewRepository;
  @Mock private GradeRepository gradeRepository;
  @Mock private SubmissionMapper mapper;
  @Mock private ReviewMapper reviewMapper;
  @Mock private GradeMapper gradeMapper;
  @Mock private SubmissionStateMachine stateMachine;
  @Mock private CurrentUser currentUser;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private SubmissionServiceImpl service;

  private static final Sort VERSION_DESC = SubmissionRepository.LATEST_VERSION;

  @Test
  @DisplayName("createSubmission starts at version 1 when the student has no prior submission")
  void createSubmission_firstVersion() {
    UUID assignmentId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    CreateSubmissionRequest request = mock(CreateSubmissionRequest.class);
    Submission saved = Submission.builder().version(1).build();
    SubmissionResponse dto = mock(SubmissionResponse.class);
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findFirstByAssignmentIdAndStudentId(assignmentId, studentId, VERSION_DESC))
        .thenReturn(Optional.empty());
    when(mapper.toEntity(request, assignmentId, studentId, 1)).thenReturn(saved);
    when(repository.save(saved)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.createSubmission(assignmentId, request)).isSameAs(dto);
  }

  @Test
  @DisplayName("createSubmission uses the next version when prior submissions exist")
  void createSubmission_nextVersion() {
    UUID assignmentId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    CreateSubmissionRequest request = mock(CreateSubmissionRequest.class);
    Submission previous = Submission.builder().version(2).build();
    Submission saved = Submission.builder().version(3).build();
    SubmissionResponse dto = mock(SubmissionResponse.class);
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findFirstByAssignmentIdAndStudentId(assignmentId, studentId, VERSION_DESC))
        .thenReturn(Optional.of(previous));
    when(mapper.toEntity(request, assignmentId, studentId, 3)).thenReturn(saved);
    when(repository.save(saved)).thenReturn(saved);
    when(mapper.toResponse(saved)).thenReturn(dto);

    assertThat(service.createSubmission(assignmentId, request)).isSameAs(dto);
  }

  @Test
  @DisplayName("submitSubmission transitions the submission to SUBMITTED")
  void submitSubmission_transitions() {
    UUID submissionId = UUID.randomUUID();
    Submission submission = Submission.builder().version(1).build();
    SubmissionResponse dto = mock(SubmissionResponse.class);
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(repository.save(submission)).thenReturn(submission);
    when(mapper.toResponse(submission)).thenReturn(dto);

    assertThat(service.submitSubmission(submissionId)).isSameAs(dto);
    verify(stateMachine).transitionTo(submission, SubmissionStatus.SUBMITTED);
  }

  @Test
  @DisplayName("updateSubmission asserts the submission is editable, then saves the new content")
  void updateSubmission_editsDraft() {
    UUID submissionId = UUID.randomUUID();
    Submission submission = Submission.builder().version(1).build();
    SubmissionResponse dto = mock(SubmissionResponse.class);
    var request = mock(de.codillas.homework.api.dto.UpdateSubmissionRequest.class);
    when(request.getContent()).thenReturn("new text");
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(repository.save(submission)).thenReturn(submission);
    when(mapper.toResponse(submission)).thenReturn(dto);

    assertThat(service.updateSubmission(submissionId, request)).isSameAs(dto);
    verify(stateMachine).assertEditable(submission);
    assertThat(submission.getContent()).isEqualTo("new text");
  }

  @Test
  @DisplayName("reviewSubmission moves the submission into review and saves the review")
  void reviewSubmission_transitionsAndSaves() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Submission submission = Submission.builder().version(1).build();
    CreateReviewRequest request = mock(CreateReviewRequest.class);
    Review review =
        Review.builder().submissionId(submissionId).reviewerId(reviewerId).comment("ok").build();
    ReviewResponse dto = mock(ReviewResponse.class);
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(currentUser.id()).thenReturn(reviewerId);
    when(reviewMapper.toEntity(request, submissionId, reviewerId)).thenReturn(review);
    when(reviewRepository.save(review)).thenReturn(review);
    when(reviewMapper.toResponse(review)).thenReturn(dto);

    assertThat(service.reviewSubmission(submissionId, request)).isSameAs(dto);
    verify(stateMachine).transitionTo(submission, SubmissionStatus.IN_REVIEW);
  }

  @Test
  @DisplayName("gradeSubmission grades, saves and publishes SubmissionGraded")
  void gradeSubmission_transitionsSavesAndPublishesEvent() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID teacherId = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(studentId).version(1).build();
    CreateGradeRequest request = mock(CreateGradeRequest.class);
    Grade grade = Grade.builder().submissionId(submissionId).score(90).gradedBy(teacherId).build();
    GradeResponse dto = mock(GradeResponse.class);
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(currentUser.id()).thenReturn(teacherId);
    when(gradeMapper.toEntity(request, submissionId, teacherId)).thenReturn(grade);
    when(gradeRepository.save(grade)).thenReturn(grade);
    when(gradeMapper.toResponse(grade)).thenReturn(dto);

    assertThat(service.gradeSubmission(submissionId, request)).isSameAs(dto);
    verify(stateMachine).transitionTo(submission, SubmissionStatus.GRADED);
    verify(events).publishEvent(new SubmissionGraded(submissionId, assignmentId, studentId, 90));
  }

  @Test
  @DisplayName("returnSubmission transitions the submission to RETURNED")
  void returnSubmission_transitions() {
    UUID submissionId = UUID.randomUUID();
    Submission submission = Submission.builder().version(1).build();
    SubmissionResponse dto = mock(SubmissionResponse.class);
    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(repository.save(submission)).thenReturn(submission);
    when(mapper.toResponse(submission)).thenReturn(dto);

    assertThat(service.returnSubmission(submissionId)).isSameAs(dto);
    verify(stateMachine).transitionTo(submission, SubmissionStatus.RETURNED);
  }

  @Test
  @DisplayName("listSubmissions maps the assignment's submissions ordered by student and version")
  void listSubmissions_mapsList() {
    UUID assignmentId = UUID.randomUUID();
    List<Submission> submissions = List.of(Submission.builder().version(1).build());
    List<SubmissionResponse> dtos = List.of(mock(SubmissionResponse.class));
    when(repository.findByAssignmentId(eq(assignmentId), any(Sort.class))).thenReturn(submissions);
    when(mapper.toResponseList(submissions)).thenReturn(dtos);

    assertThat(service.listSubmissions(assignmentId)).isSameAs(dtos);
  }
}
