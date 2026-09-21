package de.codillas.homework.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CreateReviewRequest;
import de.codillas.homework.api.dto.CreateSubmissionRequest;
import de.codillas.homework.api.dto.CriterionScoreInput;
import de.codillas.homework.api.dto.GradeResponse;
import de.codillas.homework.api.dto.ReviewResponse;
import de.codillas.homework.api.dto.SubmissionResponse;
import de.codillas.homework.api.dto.UpdateSubmissionRequest;
import de.codillas.homework.domain.GradeCalculator;
import de.codillas.homework.domain.SubmissionStateMachine;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.Grade;
import de.codillas.homework.domain.model.GradeCriterion;
import de.codillas.homework.domain.model.Review;
import de.codillas.homework.domain.model.RubricCriterion;
import de.codillas.homework.domain.model.Submission;
import de.codillas.homework.domain.model.SubmissionStatus;
import de.codillas.homework.domain.repository.AssignmentRepository;
import de.codillas.homework.domain.repository.GradeCriterionRepository;
import de.codillas.homework.domain.repository.GradeRepository;
import de.codillas.homework.domain.repository.ReviewRepository;
import de.codillas.homework.domain.repository.RubricCriterionRepository;
import de.codillas.homework.domain.repository.SubmissionRepository;
import de.codillas.homework.mapper.GradeMapper;
import de.codillas.homework.mapper.ReviewMapper;
import de.codillas.homework.mapper.SubmissionMapper;
import de.codillas.shared.event.SubmissionGraded;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionServiceImpl implements SubmissionService {

  private final SubmissionRepository repository;
  private final ReviewRepository reviewRepository;
  private final GradeRepository gradeRepository;
  private final AssignmentRepository assignmentRepository;
  private final RubricCriterionRepository rubricCriterionRepository;
  private final GradeCriterionRepository gradeCriterionRepository;
  private final GradeCalculator gradeCalculator;
  private final SubmissionMapper mapper;
  private final ReviewMapper reviewMapper;
  private final GradeMapper gradeMapper;
  private final SubmissionStateMachine stateMachine;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public SubmissionResponse createSubmission(UUID assignmentId, CreateSubmissionRequest request) {
    if (!assignmentRepository.existsById(assignmentId)) {
      throw new NotFoundException("Assignment", assignmentId);
    }
    UUID studentId = currentUser.id();
    int nextVersion =
        repository
                .findFirstByAssignmentIdAndStudentId(
                    assignmentId, studentId, SubmissionRepository.LATEST_VERSION)
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
    Submission submission = findByIdForCaller(submissionId);
    stateMachine.assertEditable(submission);
    submission.setContent(request.getContent());
    return mapper.toResponse(repository.save(submission));
  }

  @Override
  @Transactional
  public SubmissionResponse submitSubmission(UUID submissionId) {
    Submission submission = findByIdForCaller(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.SUBMITTED);
    Instant submittedAt = Instant.now();
    // Flag, don't block: an assignment with no due date simply means "not late".
    submission.setSubmittedAt(submittedAt);
    submission.setLate(
        assignmentRepository
            .findById(submission.getAssignmentId())
            .map(Assignment::getDueAt)
            .map(submittedAt::isAfter)
            .orElse(false));
    return mapper.toResponse(repository.save(submission));
  }

  @Override
  public List<SubmissionResponse> listSubmissions(UUID assignmentId) {
    Sort order = SubmissionRepository.BY_STUDENT_THEN_VERSION;
    return mapper.toResponseList(
        currentUser.isStaff()
            ? repository.findByAssignmentId(assignmentId, order)
            : repository.findByAssignmentIdAndStudentId(assignmentId, currentUser.id(), order));
  }

  @Override
  @Transactional
  public ReviewResponse reviewSubmission(UUID submissionId, CreateReviewRequest request) {
    Submission submission = findByIdForCaller(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.IN_REVIEW);
    repository.save(submission);
    Review review =
        reviewRepository.save(reviewMapper.toEntity(request, submissionId, currentUser.id()));
    return reviewMapper.toResponse(review);
  }

  @Override
  @Transactional
  public GradeResponse gradeSubmission(UUID submissionId, CreateGradeRequest request) {
    Submission submission = findByIdForCaller(submissionId);
    // Flag, don't block: a missing assignment row means no rubric, no late penalty and no group —
    // grade the submission as a flat score (mirrors submitSubmission's tolerance of a missing row).
    Optional<Assignment> assignment = assignmentRepository.findById(submission.getAssignmentId());
    if (submission.getStatus() != SubmissionStatus.GRADED) {
      stateMachine.transitionTo(submission, SubmissionStatus.GRADED);
      repository.save(submission);
    }

    RawScore raw = rawScore(assignment.map(Assignment::getRubricId).orElse(null), request);
    long daysLate = assignment.map(a -> lateDays(a, submission)).orElse(0L);
    int effective =
        gradeCalculator.effectiveScore(
            raw.points(),
            daysLate,
            assignment.map(Assignment::getLatePenaltyPctPerDay).orElse(null),
            assignment.map(Assignment::getMaxLatePenaltyPct).orElse(null));

    Grade grade =
        gradeRepository
            .findBySubmissionId(submissionId)
            .orElseGet(() -> Grade.builder().submissionId(submissionId).build());
    grade.setGradedBy(currentUser.id());
    grade.setScore(raw.points());
    grade.setMaxPoints(raw.maxPoints());
    grade.setEffectiveScore(effective);
    Grade saved = gradeRepository.save(grade);

    gradeCriterionRepository.deleteByGradeId(saved.getId());
    List<GradeCriterion> breakdown =
        raw.criteria().entrySet().stream()
            .<GradeCriterion>map(
                e ->
                    GradeCriterion.builder()
                        .gradeId(saved.getId())
                        .criterionId(e.getKey())
                        .points(e.getValue())
                        .build())
            .toList();
    gradeCriterionRepository.saveAll(breakdown);

    events.publishEvent(
        new SubmissionGraded(
            submissionId,
            submission.getAssignmentId(),
            submission.getStudentId(),
            effective,
            raw.maxPoints(),
            assignment.map(Assignment::getGroupId).orElse(null)));
    return gradeMapper.toResponse(saved, gradeCriterionRepository.findByGradeId(saved.getId()));
  }

  /** Raw points + denominator for one grade, from the rubric when present, else the flat score. */
  private RawScore rawScore(@Nullable UUID rubricId, CreateGradeRequest request) {
    if (rubricId == null) {
      if (request.getScore() == null) {
        throw new BadRequestException("score is required when the assignment has no rubric");
      }
      return new RawScore(request.getScore(), 100, Map.of());
    }
    List<RubricCriterion> criteria =
        rubricCriterionRepository.findByRubricId(rubricId, RubricCriterionRepository.BY_POSITION);
    Map<UUID, Integer> maxById =
        criteria.stream()
            .collect(Collectors.toMap(RubricCriterion::getId, RubricCriterion::getMaxPoints));
    Map<UUID, Integer> given = new LinkedHashMap<>();
    for (CriterionScoreInput in :
        Optional.ofNullable(request.getCriterionScores()).orElse(List.of())) {
      Integer max = maxById.get(in.getCriterionId());
      if (max == null) {
        throw new BadRequestException("Unknown rubric criterion " + in.getCriterionId());
      }
      if (in.getPoints() > max) {
        throw new BadRequestException("Criterion score exceeds its maxPoints");
      }
      given.put(in.getCriterionId(), in.getPoints());
    }
    int points = given.values().stream().mapToInt(Integer::intValue).sum();
    int maxPoints = criteria.stream().mapToInt(RubricCriterion::getMaxPoints).sum();
    return new RawScore(points, maxPoints, given);
  }

  /** Whole days late (ceiling), 0 when on time or P1 late metadata is absent. */
  private long lateDays(Assignment assignment, Submission submission) {
    if (!submission.isLate()
        || submission.getSubmittedAt() == null
        || assignment.getDueAt() == null) {
      return 0;
    }
    long minutes = Duration.between(assignment.getDueAt(), submission.getSubmittedAt()).toMinutes();
    return minutes <= 0 ? 0 : (long) Math.ceil(minutes / 1440.0);
  }

  private record RawScore(int points, int maxPoints, Map<UUID, Integer> criteria) {}

  @Override
  @Transactional
  public SubmissionResponse returnSubmission(UUID submissionId) {
    Submission submission = findByIdForCaller(submissionId);
    stateMachine.transitionTo(submission, SubmissionStatus.RETURNED);
    return mapper.toResponse(repository.save(submission));
  }

  @Override
  @Transactional
  public void onGroupDeleted(UUID groupId) {
    List<UUID> assignmentIds =
        assignmentRepository.findByGroupId(groupId).stream().map(Assignment::getId).toList();
    if (assignmentIds.isEmpty()) {
      return;
    }
    List<UUID> submissionIds =
        repository.findByAssignmentIdIn(assignmentIds).stream().map(Submission::getId).toList();
    if (!submissionIds.isEmpty()) {
      gradeRepository.deleteBySubmissionIdIn(submissionIds);
      reviewRepository.deleteBySubmissionIdIn(submissionIds);
      repository.deleteByAssignmentIdIn(assignmentIds);
    }
    assignmentRepository.deleteByGroupId(groupId);
  }

  /**
   * Load a submission the caller is entitled to: its owner (the student) or any staff member. A
   * non-owner non-staff caller is told it does not exist (404, no ownership leak) — mirrors {@code
   * chat.requireMember}.
   */
  private Submission findByIdForCaller(UUID id) {
    Submission submission =
        repository.findById(id).orElseThrow(() -> new NotFoundException("Submission", id));
    if (!currentUser.isStaff() && !submission.getStudentId().equals(currentUser.id())) {
      throw new NotFoundException("Submission", id);
    }
    return submission;
  }
}
