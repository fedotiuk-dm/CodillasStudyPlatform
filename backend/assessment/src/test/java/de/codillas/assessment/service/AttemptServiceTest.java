package de.codillas.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.assessment.api.dto.AnswerResponse;
import de.codillas.assessment.api.dto.AttemptResponse;
import de.codillas.assessment.api.dto.GradeAnswerRequest;
import de.codillas.assessment.api.dto.SaveAnswerRequest;
import de.codillas.assessment.domain.AttemptGrader;
import de.codillas.assessment.domain.AttemptStateMachine;
import de.codillas.assessment.domain.model.Answer;
import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.AttemptStatus;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.assessment.domain.repository.AnswerRepository;
import de.codillas.assessment.domain.repository.AttemptRepository;
import de.codillas.assessment.domain.repository.OptionRepository;
import de.codillas.assessment.domain.repository.QuestionRepository;
import de.codillas.assessment.domain.repository.TestRepository;
import de.codillas.assessment.mapper.AttemptMapper;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttemptService")
class AttemptServiceTest {

  @Mock private AttemptRepository repository;
  @Mock private AnswerRepository answerRepository;
  @Mock private TestRepository testRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private OptionRepository optionRepository;
  @Mock private AttemptMapper mapper;
  @Mock private AttemptStateMachine stateMachine;
  @Mock private AttemptGrader grader;
  @Mock private CurrentUser currentUser;
  @Mock private ApplicationEventPublisher events;
  @InjectMocks private AttemptServiceImpl service;

  @Test
  @DisplayName("startAttempt fails when a brand-new attempt targets an unpublished test")
  void startAttempt_notPublished() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.DRAFT)
                    .build()));
    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.startAttempt(testId));
  }

  @Test
  @DisplayName("startAttempt resumes the student's in-progress attempt instead of creating one")
  void startAttempt_resumesInProgress() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID attemptId = UUID.randomUUID();
    Attempt existing = Attempt.builder().id(attemptId).testId(testId).studentId(studentId).build();
    AttemptResponse dto = mock(AttemptResponse.class);

    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.of(existing));
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
    when(mapper.toAnswerResponses(List.of())).thenReturn(List.of());
    when(mapper.toResponse(eq(existing), any())).thenReturn(dto);

    assertThat(service.startAttempt(testId)).isSameAs(dto);
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("startAttempt creates the next attempt number once previous ones are finished")
  void startAttempt_createsNextNumber() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Attempt first = Attempt.builder().attemptNumber(1).status(AttemptStatus.GRADED).build();
    Attempt created = Attempt.builder().id(UUID.randomUUID()).build();
    AttemptResponse dto = mock(AttemptResponse.class);

    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .build()));
    when(repository.findByTestIdAndStudentId(testId, studentId)).thenReturn(List.of(first));
    when(mapper.toEntity(eq(testId), eq(studentId), eq(2), any())).thenReturn(created);
    when(repository.save(created)).thenReturn(created);
    when(mapper.toResponse(eq(created), any())).thenReturn(dto);

    assertThat(service.startAttempt(testId)).isSameAs(dto);
    verify(mapper).toEntity(eq(testId), eq(studentId), eq(2), any());
  }

  @Test
  @DisplayName("startAttempt rejects a new attempt once the cap is reached")
  void startAttempt_capReached() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .maxAttempts(2)
                    .build()));
    when(repository.findByTestIdAndStudentId(testId, studentId))
        .thenReturn(
            List.of(
                Attempt.builder().attemptNumber(1).status(AttemptStatus.GRADED).build(),
                Attempt.builder().attemptNumber(2).status(AttemptStatus.GRADED).build()));

    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.startAttempt(testId));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("startAttempt rejects when the test's availability window has closed")
  void startAttempt_windowClosed() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .availableUntil(java.time.Instant.now().minusSeconds(60))
                    .build()));

    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.startAttempt(testId));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("startAttempt rejects when the test's availability window has not yet opened")
  void startAttempt_beforeAvailableFrom() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .availableFrom(java.time.Instant.now().plusSeconds(60))
                    .build()));

    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.startAttempt(testId));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("startAttempt succeeds inside the [availableFrom, availableUntil] window")
  void startAttempt_insideWindow_succeeds() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Attempt created = Attempt.builder().id(UUID.randomUUID()).build();
    AttemptResponse dto = mock(AttemptResponse.class);

    when(currentUser.id()).thenReturn(studentId);
    when(repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS))
        .thenReturn(Optional.empty());
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .availableFrom(java.time.Instant.now().minusSeconds(60))
                    .availableUntil(java.time.Instant.now().plusSeconds(60))
                    .build()));
    when(repository.findByTestIdAndStudentId(testId, studentId)).thenReturn(List.of());
    when(mapper.toEntity(eq(testId), eq(studentId), eq(1), any())).thenReturn(created);
    when(repository.save(created)).thenReturn(created);
    when(mapper.toResponse(eq(created), any())).thenReturn(dto);

    assertThat(service.startAttempt(testId)).isSameAs(dto);
    verify(repository).save(created);
  }

  @Test
  @DisplayName("submitAttempt auto-grades answers, scores the attempt and emits AttemptCompleted")
  void submitAttempt_gradesAndPublishes() {
    UUID attemptId = UUID.randomUUID();
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).testId(testId).studentId(studentId).build();
    Answer answer = Answer.builder().questionId(questionId).build();
    Question question = Question.builder().id(questionId).build();
    AttemptResponse dto = mock(AttemptResponse.class);

    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(true);
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of(answer));
    when(questionRepository.findByTestId(eq(testId), any())).thenReturn(List.of(question));
    when(optionRepository.findByQuestionIdIn(any()))
        .thenReturn(List.of(Option.builder().questionId(questionId).build()));
    when(grader.autoScore(eq(question), anyList(), eq(answer))).thenReturn(5);
    when(grader.totalScore(any())).thenReturn(5);
    when(grader.allGraded(any())).thenReturn(true);
    when(mapper.toResponse(eq(attempt), any())).thenReturn(dto);

    assertThat(service.submitAttempt(attemptId)).isSameAs(dto);
    assertThat(answer.getAwardedPoints()).isEqualTo(5);
    assertThat(attempt.getScore()).isEqualTo(5);
    verify(stateMachine).transitionTo(attempt, AttemptStatus.GRADED);
    verify(events).publishEvent(new AttemptCompleted(attemptId, testId, studentId, 5, 0, null));
  }

  @Test
  @DisplayName("submitAttempt that still has ungraded text answers stays SUBMITTED")
  void submitAttempt_pendingManual_staysSubmitted() {
    UUID attemptId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder()
            .id(attemptId)
            .testId(UUID.randomUUID())
            .studentId(UUID.randomUUID())
            .build();
    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(true);
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
    when(questionRepository.findByTestId(any(), any())).thenReturn(List.of());
    when(grader.totalScore(any())).thenReturn(0);
    when(grader.allGraded(any())).thenReturn(false);
    when(mapper.toResponse(any(), any())).thenReturn(mock(AttemptResponse.class));

    service.submitAttempt(attemptId);
    verify(stateMachine).transitionTo(attempt, AttemptStatus.SUBMITTED);
  }

  @Test
  @DisplayName("saveAnswer past the time limit auto-finalizes the attempt and is rejected (409)")
  void saveAnswer_expired_finalizesAndRejects() {
    UUID attemptId = UUID.randomUUID();
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder()
            .id(attemptId)
            .testId(testId)
            .studentId(studentId)
            .startedAt(java.time.Instant.now().minus(java.time.Duration.ofHours(2)))
            .build();

    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(true);
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder().durationMinutes(60).build()));
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
    when(questionRepository.findByTestId(eq(testId), any())).thenReturn(List.of());
    when(grader.totalScore(any())).thenReturn(0);
    when(grader.allGraded(any())).thenReturn(true);
    when(mapper.toResponse(any(), any())).thenReturn(mock(AttemptResponse.class));

    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.saveAnswer(attemptId, mock(SaveAnswerRequest.class)));
    verify(stateMachine).transitionTo(attempt, AttemptStatus.GRADED);
    verify(events).publishEvent(any(AttemptCompleted.class));
    verify(answerRepository, never()).save(any());
  }

  @Test
  @DisplayName("saveAnswer inside the timer window persists the answer and does not finalize")
  void saveAnswer_withinWindow_succeeds() {
    UUID attemptId = UUID.randomUUID();
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID questionId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder()
            .id(attemptId)
            .testId(testId)
            .studentId(studentId)
            .status(AttemptStatus.IN_PROGRESS)
            .startedAt(java.time.Instant.now())
            .build();
    Answer saved = Answer.builder().attemptId(attemptId).questionId(questionId).build();
    de.codillas.assessment.api.dto.AnswerResponse dto =
        mock(de.codillas.assessment.api.dto.AnswerResponse.class);
    SaveAnswerRequest request = mock(SaveAnswerRequest.class);
    when(request.getQuestionId()).thenReturn(questionId);

    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(true);
    // 60-minute test, started just now -> still well inside the window.
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder().durationMinutes(60).build()));
    when(answerRepository.findByAttemptIdAndQuestionId(attemptId, questionId))
        .thenReturn(Optional.empty());
    when(mapper.toAnswer(request, attemptId)).thenReturn(saved);
    when(answerRepository.save(saved)).thenReturn(saved);
    when(mapper.toAnswerResponse(saved)).thenReturn(dto);

    assertThat(service.saveAnswer(attemptId, request)).isSameAs(dto);
    verify(answerRepository).save(saved);
    // In-window save must NOT auto-finalize the attempt.
    verify(stateMachine, never()).transitionTo(any(), any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  @DisplayName("the owner may read their own attempt")
  void getAttempt_owner_allowed() {
    UUID attemptId = UUID.randomUUID();
    UUID owner = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).studentId(owner).build();
    AttemptResponse dto = mock(AttemptResponse.class);
    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(owner);
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
    when(mapper.toResponse(eq(attempt), any())).thenReturn(dto);

    assertThat(service.getAttempt(attemptId)).isSameAs(dto);
  }

  @Test
  @DisplayName("another student reading the attempt gets 404")
  void getAttempt_otherStudent_notFound() {
    UUID attemptId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(attemptId).studentId(UUID.randomUUID()).build();
    when(repository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(currentUser.isStaff()).thenReturn(false);
    when(currentUser.id()).thenReturn(UUID.randomUUID());

    assertThatExceptionOfType(de.codillas.shared.exception.NotFoundException.class)
        .isThrownBy(() -> service.getAttempt(attemptId));
  }

  @Test
  @DisplayName("listTestAttempts returns every attempt at the test with its answers")
  void listTestAttempts_mapsEachAttemptWithAnswers() {
    UUID testId = UUID.randomUUID();
    Attempt attempt = Attempt.builder().id(UUID.randomUUID()).testId(testId).build();
    Answer answer = Answer.builder().attemptId(attempt.getId()).build();
    AttemptResponse dto = mock(AttemptResponse.class);
    when(repository.findByTestId(testId, AttemptRepository.BY_STUDENT_THEN_NUMBER))
        .thenReturn(List.of(attempt));
    when(answerRepository.findByAttemptId(attempt.getId())).thenReturn(List.of(answer));
    when(mapper.toAnswerResponses(List.of(answer))).thenReturn(List.of());
    when(mapper.toResponse(attempt, List.of())).thenReturn(dto);

    assertThat(service.listTestAttempts(testId)).containsExactly(dto);
  }

  @Test
  @DisplayName(
      "gradeAnswer that grades the last pending answer finishes the attempt and re-emits AttemptCompleted")
  void gradeAnswer_lastAnswer_gradesAndPublishes() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Attempt attempt =
        Attempt.builder()
            .id(UUID.randomUUID())
            .testId(testId)
            .studentId(studentId)
            .status(AttemptStatus.SUBMITTED)
            .build();
    Answer answer = Answer.builder().id(UUID.randomUUID()).attemptId(attempt.getId()).build();
    GradeAnswerRequest request = new GradeAnswerRequest(3);
    when(currentUser.isStaff()).thenReturn(true);
    when(repository.findById(attempt.getId())).thenReturn(Optional.of(attempt));
    when(answerRepository.findById(answer.getId())).thenReturn(Optional.of(answer));
    when(answerRepository.findByAttemptId(attempt.getId())).thenReturn(List.of(answer));
    when(grader.totalScore(List.of(answer))).thenReturn(3);
    when(grader.allGraded(List.of(answer))).thenReturn(true);
    when(questionRepository.findByTestId(testId, QuestionRepository.BY_ORDER))
        .thenReturn(List.of(Question.builder().points(5).build()));
    when(mapper.toAnswerResponse(answer)).thenReturn(mock(AnswerResponse.class));

    service.gradeAnswer(attempt.getId(), answer.getId(), request);

    assertThat(answer.getAwardedPoints()).isEqualTo(3);
    verify(stateMachine).transitionTo(attempt, AttemptStatus.GRADED);
    verify(events)
        .publishEvent(new AttemptCompleted(attempt.getId(), testId, studentId, 3, 5, null));
  }
}
