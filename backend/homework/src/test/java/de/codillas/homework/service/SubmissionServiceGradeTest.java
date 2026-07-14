package de.codillas.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.homework.api.dto.CreateGradeRequest;
import de.codillas.homework.api.dto.CriterionScoreInput;
import de.codillas.homework.domain.GradeCalculator;
import de.codillas.homework.domain.SubmissionStateMachine;
import de.codillas.homework.domain.model.Assignment;
import de.codillas.homework.domain.model.Grade;
import de.codillas.homework.domain.model.GradeCriterion;
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
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmissionServiceImpl — grade computation")
class SubmissionServiceGradeTest {

  @Mock SubmissionRepository repository;
  @Mock ReviewRepository reviewRepository;
  @Mock GradeRepository gradeRepository;
  @Mock AssignmentRepository assignmentRepository;
  @Mock RubricCriterionRepository rubricCriterionRepository;
  @Mock GradeCriterionRepository gradeCriterionRepository;
  @Mock GradeCalculator gradeCalculator;
  @Mock SubmissionMapper mapper;
  @Mock ReviewMapper reviewMapper;
  @Mock GradeMapper gradeMapper;
  @Mock SubmissionStateMachine stateMachine;
  @Mock CurrentUser currentUser;
  @Mock ApplicationEventPublisher events;
  @InjectMocks SubmissionServiceImpl service;

  private static RubricCriterion criterion(UUID id, UUID rubricId, int max, int position) {
    RubricCriterion c =
        RubricCriterion.builder().rubricId(rubricId).maxPoints(max).position(position).build();
    c.setId(id);
    return c;
  }

  @Test
  @DisplayName("re-grading a GRADED submission updates the existing grade and skips the transition")
  void reGradeUpserts() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID teacher = UUID.randomUUID();
    Submission graded =
        Submission.builder().assignmentId(assignmentId).studentId(UUID.randomUUID()).build();
    graded.setStatus(SubmissionStatus.GRADED);
    Grade existing = Grade.builder().submissionId(submissionId).score(60).gradedBy(teacher).build();
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();

    when(repository.findById(submissionId)).thenReturn(Optional.of(graded));
    when(currentUser.isStaff()).thenReturn(true);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(existing));
    when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.id()).thenReturn(teacher);
    when(gradeCalculator.effectiveScore(eq(95), eq(0L), any(), any())).thenReturn(95);

    service.gradeSubmission(submissionId, new CreateGradeRequest().score(95));

    assertThat(existing.getScore()).isEqualTo(95); // mutated, not recreated
    assertThat(existing.getEffectiveScore()).isEqualTo(95);
    assertThat(existing.getMaxPoints()).isEqualTo(100);
    verifyNoInteractions(stateMachine); // already GRADED -> no transition
  }

  @Test
  @DisplayName("a flat, on-time submission keeps the raw score as the effective score over 100")
  void gradeSubmission_flatOnTime() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(UUID.randomUUID()).build();
    submission.setStatus(SubmissionStatus.SUBMITTED);
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();

    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(currentUser.isStaff()).thenReturn(true);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(gradeCalculator.effectiveScore(eq(88), eq(0L), any(), any())).thenReturn(88);

    service.gradeSubmission(submissionId, new CreateGradeRequest().score(88));

    ArgumentCaptor<Grade> grade = ArgumentCaptor.forClass(Grade.class);
    verify(gradeRepository).save(grade.capture());
    assertThat(grade.getValue().getScore()).isEqualTo(88);
    assertThat(grade.getValue().getEffectiveScore()).isEqualTo(88);
    assertThat(grade.getValue().getMaxPoints()).isEqualTo(100);
    verify(stateMachine).transitionTo(submission, SubmissionStatus.GRADED);
  }

  @Test
  @DisplayName(
      "a late submission runs the raw score through GradeCalculator with the whole days late")
  void gradeSubmission_flatLatePenalty() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    Instant due = Instant.parse("2026-01-01T00:00:00Z");
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(UUID.randomUUID()).build();
    submission.setStatus(SubmissionStatus.SUBMITTED);
    submission.setLate(true);
    submission.setSubmittedAt(due.plus(Duration.ofDays(1)));
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();
    assignment.setDueAt(due);
    assignment.setLatePenaltyPctPerDay(10);
    assignment.setMaxLatePenaltyPct(50);

    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(currentUser.isStaff()).thenReturn(true);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(gradeCalculator.effectiveScore(80, 1L, 10, 50)).thenReturn(72);

    service.gradeSubmission(submissionId, new CreateGradeRequest().score(80));

    ArgumentCaptor<Grade> grade = ArgumentCaptor.forClass(Grade.class);
    verify(gradeRepository).save(grade.capture());
    assertThat(grade.getValue().getScore()).isEqualTo(80);
    assertThat(grade.getValue().getEffectiveScore()).isEqualTo(72);
    verify(gradeCalculator).effectiveScore(80, 1L, 10, 50);
  }

  @Test
  @DisplayName("gradeSubmission with a rubric scores Σ criterion points over Σ max points")
  void gradeSubmission_rubric() {
    UUID submissionId = UUID.randomUUID();
    UUID assignmentId = UUID.randomUUID();
    UUID rubricId = UUID.randomUUID();
    UUID cA = UUID.randomUUID();
    UUID cB = UUID.randomUUID();
    Submission submission =
        Submission.builder().assignmentId(assignmentId).studentId(UUID.randomUUID()).build();
    submission.setStatus(SubmissionStatus.SUBMITTED);
    Assignment assignment = Assignment.builder().groupId(UUID.randomUUID()).title("HW").build();
    assignment.setRubricId(rubricId);
    CreateGradeRequest request =
        new CreateGradeRequest()
            .addCriterionScoresItem(new CriterionScoreInput().criterionId(cA).points(24))
            .addCriterionScoresItem(new CriterionScoreInput().criterionId(cB).points(8));

    when(repository.findById(submissionId)).thenReturn(Optional.of(submission));
    when(currentUser.isStaff()).thenReturn(true);
    when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
    when(rubricCriterionRepository.findByRubricId(eq(rubricId), any()))
        .thenReturn(List.of(criterion(cA, rubricId, 30, 0), criterion(cB, rubricId, 10, 1)));
    when(gradeRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> inv.getArgument(0));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(gradeCalculator.effectiveScore(eq(32), eq(0L), any(), any())).thenReturn(32);

    service.gradeSubmission(submissionId, request);

    ArgumentCaptor<Grade> grade = ArgumentCaptor.forClass(Grade.class);
    verify(gradeRepository).save(grade.capture());
    assertThat(grade.getValue().getScore()).isEqualTo(32);
    assertThat(grade.getValue().getMaxPoints()).isEqualTo(40);
    assertThat(grade.getValue().getEffectiveScore()).isEqualTo(32);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<GradeCriterion>> breakdown = ArgumentCaptor.forClass(List.class);
    verify(gradeCriterionRepository).saveAll(breakdown.capture());
    assertThat(breakdown.getValue()).hasSize(2);
  }
}
