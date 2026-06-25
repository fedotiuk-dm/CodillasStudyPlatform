package de.codillas.homework.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.api.dto.UpdateSubmissionRequest;
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
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

  private final SubmissionRepository repository;
  private final ReviewRepository reviewRepository;
  private final GradeRepository gradeRepository;
  private final SubmissionMapper mapper;
  private final ReviewMapper reviewMapper;
  private final GradeMapper gradeMapper;
  private final SubmissionStateMachine stateMachine;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public SubmissionResponse createSubmission(UUID assignmentId, CreateSubmissionRequest request) {
    UUID studentId = currentUser.id();
    int nextVersion =
        repository
                .findFirstByAssignmentIdAndStudentId(
                    assignmentId, studentId, Sort.by(Sort.Direction.DESC, "version"))
                .map(Submission::getVersion)
                .orElse(0)
            + 1;
    Submission saved =
        repository.save(mapper.toEntity(request, assignmentId, studentId, nextVersion));
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SubmissionResponse updateSubmission(UUID submissionId, UpdateSubmissionRequest request) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.assertEditable(submission);
    submission.setContent(request.getContent());
    return mapper.toResponse(repository.save(submission));
  }

  @Override
  @Transactional
  public SubmissionResponse submitSubmission(UUID submissionId) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.SUBMITTED);
    return mapper.toResponse(repository.save(submission));
  }

  @Override
  public List<SubmissionResponse> listSubmissions(UUID assignmentId) {
    return mapper.toResponseList(
        repository.findByAssignmentId(assignmentId, Sort.by("studentId", "version")));
  }

  @Override
  @Transactional
  public ReviewResponse reviewSubmission(UUID submissionId, CreateReviewRequest request) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.IN_REVIEW);
    repository.save(submission);
    Review review =
        reviewRepository.save(reviewMapper.toEntity(request, submissionId, currentUser.id()));
    return reviewMapper.toResponse(review);
  }

  @Override
  @Transactional
  public GradeResponse gradeSubmission(UUID submissionId, CreateGradeRequest request) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.GRADED);
    repository.save(submission);
    Grade grade =
        gradeRepository.save(gradeMapper.toEntity(request, submissionId, currentUser.id()));
    events.publishEvent(
        new SubmissionGraded(
            submissionId,
            submission.getAssignmentId(),
            submission.getStudentId(),
            grade.getScore()));
    return gradeMapper.toResponse(grade);
  }

  @Override
  @Transactional
  public SubmissionResponse returnSubmission(UUID submissionId) {
    Submission submission = findByIdOrThrow(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.RETURNED);
    return mapper.toResponse(repository.save(submission));
  }

  private Submission findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Submission", id));
  }
}
