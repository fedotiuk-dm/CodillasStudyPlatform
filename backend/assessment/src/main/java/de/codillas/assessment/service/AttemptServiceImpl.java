package de.codillas.assessment.service;

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
    // One attempt per (test, student): resume the existing one (IN_PROGRESS to continue, or
    // SUBMITTED/GRADED so the caller shows the result) instead of creating a duplicate.
    Optional<Attempt> existing = repository.findByTestIdAndStudentId(testId, studentId);
    if (existing.isPresent()) {
      Attempt attempt = existing.get();
      return mapper.toResponse(
          attempt, mapper.toAnswerResponses(answerRepository.findByAttemptId(attempt.getId())));
    }
    Test test =
        testRepository.findById(testId).orElseThrow(() -> new NotFoundException("Test", testId));
    if (test.getStatus() != TestStatus.PUBLISHED) {
      throw new ConflictException("Test is not published");
    }
    Attempt saved = repository.save(mapper.toEntity(testId, studentId));
    return mapper.toResponse(saved, List.of());
  }

  @Override
  @Transactional
  public AnswerResponse saveAnswer(UUID attemptId, SaveAnswerRequest request) {
    Attempt attempt = findByIdOrThrow(attemptId);
    stateMachine.assertInProgress(attempt);
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
    Attempt attempt = findByIdOrThrow(attemptId);
    stateMachine.assertInProgress(attempt);

    List<Answer> answers = answerRepository.findByAttemptId(attemptId);
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

    events.publishEvent(
        new AttemptCompleted(
            attemptId, attempt.getTestId(), attempt.getStudentId(), attempt.getScore()));
    return mapper.toResponse(attempt, mapper.toAnswerResponses(answers));
  }

  @Override
  @Transactional
  public AnswerResponse gradeAnswer(UUID attemptId, UUID answerId, GradeAnswerRequest request) {
    Attempt attempt = findByIdOrThrow(attemptId);
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
    Attempt attempt = findByIdOrThrow(attemptId);
    return mapper.toResponse(
        attempt, mapper.toAnswerResponses(answerRepository.findByAttemptId(attemptId)));
  }

  private Attempt findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Attempt", id));
  }
}
