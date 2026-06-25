package de.codillas.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import de.codillas.assessment.api.dto.AttemptResponse;
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
  @DisplayName("startAttempt fails when the test is not published")
  void startAttempt_notPublished() {
    UUID testId = UUID.randomUUID();
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
  @DisplayName("startAttempt fails when the student already has an attempt")
  void startAttempt_duplicate() {
    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    when(testRepository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .build()));
    when(currentUser.id()).thenReturn(studentId);
    when(repository.existsByTestIdAndStudentId(testId, studentId)).thenReturn(true);
    assertThatExceptionOfType(de.codillas.shared.exception.ConflictException.class)
        .isThrownBy(() -> service.startAttempt(testId));
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
    verify(events).publishEvent(new AttemptCompleted(attemptId, testId, studentId, 5));
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
    when(answerRepository.findByAttemptId(attemptId)).thenReturn(List.of());
    when(questionRepository.findByTestId(any(), any())).thenReturn(List.of());
    when(grader.totalScore(any())).thenReturn(0);
    when(grader.allGraded(any())).thenReturn(false);
    when(mapper.toResponse(any(), any())).thenReturn(mock(AttemptResponse.class));

    service.submitAttempt(attemptId);
    verify(stateMachine).transitionTo(attempt, AttemptStatus.SUBMITTED);
  }
}
