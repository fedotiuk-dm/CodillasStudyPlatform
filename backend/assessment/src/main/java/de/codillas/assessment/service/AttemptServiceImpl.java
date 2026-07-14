package de.codillas.assessment.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.assessment.domain.repository.AnswerRepository;
import de.codillas.assessment.domain.repository.AttemptRepository;
import de.codillas.assessment.domain.repository.OptionRepository;
import de.codillas.assessment.domain.repository.QuestionRepository;
import de.codillas.assessment.domain.repository.TestRepository;
import de.codillas.assessment.mapper.AttemptMapper;
import de.codillas.shared.event.AttemptCompleted;
import de.codillas.shared.exception.ConflictException;
import de.codillas.shared.exception.NotFoundException;
import de.codillas.shared.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttemptServiceImpl implements AttemptService {

  private final AttemptRepository repository;
  private final AnswerRepository answerRepository;
  private final TestRepository testRepository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final AttemptMapper mapper;
  private final AttemptStateMachine stateMachine;
  private final AttemptGrader grader;
  private final CurrentUser currentUser;
  private final ApplicationEventPublisher events;

  @Override
  @Transactional
  public AttemptResponse startAttempt(UUID testId) {
    UUID studentId = currentUser.id();
    // Resume an in-progress attempt; never open a new number while one is unfinished.
    Optional<Attempt> inProgress =
        repository.findByTestIdAndStudentIdAndStatus(testId, studentId, AttemptStatus.IN_PROGRESS);
    if (inProgress.isPresent()) {
      Attempt attempt = inProgress.get();
      return mapper.toResponse(
          attempt, mapper.toAnswerResponses(answerRepository.findByAttemptId(attempt.getId())));
    }
    Test test =
        testRepository.findById(testId).orElseThrow(() -> new NotFoundException("Test", testId));
    if (test.getStatus() != TestStatus.PUBLISHED) {
      throw new ConflictException("Test is not published");
    }
    assertWithinWindow(test);

    List<Attempt> attempts = repository.findByTestIdAndStudentId(testId, studentId);
    if (test.getMaxAttempts() != null && attempts.size() >= test.getMaxAttempts()) {
      throw new ConflictException("No attempts remaining for this test");
    }
    int next = attempts.stream().mapToInt(Attempt::getAttemptNumber).max().orElse(0) + 1;
    Attempt saved = repository.save(mapper.toEntity(testId, studentId, next, Instant.now()));
    return mapper.toResponse(saved, List.of());
  }

  private void assertWithinWindow(Test test) {
    Instant now = Instant.now();
    if (test.getAvailableFrom() != null && now.isBefore(test.getAvailableFrom())) {
      throw new ConflictException("Test is not yet available");
    }
    if (test.getAvailableUntil() != null && now.isAfter(test.getAvailableUntil())) {
      throw new ConflictException("Test is no longer available");
    }
  }

  @Override
  @Transactional
  public AnswerResponse saveAnswer(UUID attemptId, SaveAnswerRequest request) {
    Attempt attempt = findByIdForCaller(attemptId);
    stateMachine.assertInProgress(attempt);
    enforceDeadline(attempt);
    Answer answer =
        answerRepository
            .findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
            .map(
                existing -> {
                  mapper.updateAnswer(existing, request);
                  return existing;
                })
            .orElseGet(() -> mapper.toAnswer(request, attemptId));
    return mapper.toAnswerResponse(answerRepository.save(answer));
  }

  @Override
  @Transactional
  public AttemptResponse submitAttempt(UUID attemptId) {
    Attempt attempt = findByIdForCaller(attemptId);
    stateMachine.assertInProgress(attempt);
    return finalizeAttempt(attempt);
  }

  /** Grade every answer, score + transition the attempt, publish AttemptCompleted. */
  private AttemptResponse finalizeAttempt(Attempt attempt) {
    List<Answer> answers = answerRepository.findByAttemptId(attempt.getId());
    Map<UUID, Question> questionsById =
        questionRepository.findByTestId(attempt.getTestId(), QuestionRepository.BY_ORDER).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));
    Map<UUID, List<Option>> optionsByQuestion =
        questionsById.isEmpty()
            ? Map.of()
            : optionRepository.findByQuestionIdIn(questionsById.keySet()).stream()
                .collect(Collectors.groupingBy(Option::getQuestionId));

    for (Answer answer : answers) {
      Question question = questionsById.get(answer.getQuestionId());
      if (question != null) {
        answer.setAwardedPoints(
            grader.autoScore(
                question, optionsByQuestion.getOrDefault(question.getId(), List.of()), answer));
      }
    }
    answerRepository.saveAll(answers);

    attempt.setScore(grader.totalScore(answers));
    stateMachine.transitionTo(
        attempt, grader.allGraded(answers) ? AttemptStatus.GRADED : AttemptStatus.SUBMITTED);
    repository.save(attempt);
    int maxPoints = questionsById.values().stream().mapToInt(Question::getPoints).sum();
    events.publishEvent(
        new AttemptCompleted(
            attempt.getId(),
            attempt.getTestId(),
            attempt.getStudentId(),
            attempt.getScore(),
            maxPoints,
            null)); // groupId resolved by gradebook from its membership read model
    return mapper.toResponse(attempt, mapper.toAnswerResponses(answers));
  }

  /** When the time limit has elapsed, auto-finalize and reject further edits. */
  private void enforceDeadline(Attempt attempt) {
    Test test =
        testRepository
            .findById(attempt.getTestId())
            .orElseThrow(() -> new NotFoundException("Test", attempt.getTestId()));
    Integer minutes = test.getDurationMinutes();
    if (minutes == null || attempt.getStartedAt() == null) {
      return;
    }
    if (Instant.now().isAfter(attempt.getStartedAt().plus(Duration.ofMinutes(minutes)))) {
      finalizeAttempt(attempt);
      throw new ConflictException("Attempt time has expired");
    }
  }

  @Override
  @Transactional
  public AnswerResponse gradeAnswer(UUID attemptId, UUID answerId, GradeAnswerRequest request) {
    Attempt attempt = findByIdForCaller(attemptId);
    Answer answer =
        answerRepository
            .findById(answerId)
            .filter(a -> a.getAttemptId().equals(attemptId))
            .orElseThrow(() -> new NotFoundException("Answer", answerId));

    answer.setAwardedPoints(request.getAwardedPoints());
    answerRepository.save(answer);

    List<Answer> answers = answerRepository.findByAttemptId(attemptId);
    attempt.setScore(grader.totalScore(answers));
    if (attempt.getStatus() == AttemptStatus.SUBMITTED && grader.allGraded(answers)) {
      stateMachine.transitionTo(attempt, AttemptStatus.GRADED);
    }
    repository.save(attempt);
    return mapper.toAnswerResponse(answer);
  }

  @Override
  public AttemptResponse getAttempt(UUID attemptId) {
    Attempt attempt = findByIdForCaller(attemptId);
    return mapper.toResponse(
        attempt, mapper.toAnswerResponses(answerRepository.findByAttemptId(attemptId)));
  }

  /** Load an attempt the caller owns, or any attempt for staff. Otherwise 404 (no leak). */
  private Attempt findByIdForCaller(UUID id) {
    Attempt attempt =
        repository.findById(id).orElseThrow(() -> new NotFoundException("Attempt", id));
    if (!currentUser.isStaff() && !attempt.getStudentId().equals(currentUser.id())) {
      throw new NotFoundException("Attempt", id);
    }
    return attempt;
  }
}
