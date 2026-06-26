package de.codillas.assessment.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.codillas.assessment.api.dto.CreateOptionRequest;
import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.CreateTestRequest;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.QuestionType;
import de.codillas.assessment.api.dto.TestListResponse;
import de.codillas.assessment.api.dto.TestResponse;
import de.codillas.assessment.domain.TestStateMachine;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.Test;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.assessment.domain.repository.OptionRepository;
import de.codillas.assessment.domain.repository.QuestionRepository;
import de.codillas.assessment.domain.repository.TestRepository;
import de.codillas.assessment.mapper.QuestionMapper;
import de.codillas.assessment.mapper.TestMapper;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestServiceImpl implements TestService {

  private final TestRepository repository;
  private final QuestionRepository questionRepository;
  private final OptionRepository optionRepository;
  private final TestMapper mapper;
  private final QuestionMapper questionMapper;
  private final TestStateMachine stateMachine;

  @Override
  @Transactional
  public TestResponse createTest(CreateTestRequest request) {
    Test saved = repository.save(mapper.toEntity(request));
    return mapper.toResponse(saved, List.of());
  }

  @Override
  @Transactional
  public TestResponse publishTest(UUID testId) {
    Test test = findByIdOrThrow(testId);
    stateMachine.transitionTo(test, TestStatus.PUBLISHED);
    return toTestResponse(repository.save(test));
  }

  @Override
  @Transactional
  public QuestionResponse addQuestion(UUID testId, CreateQuestionRequest request) {
    Test test = findByIdOrThrow(testId);
    if (test.getStatus() != TestStatus.DRAFT) {
      throw new BadRequestException("Questions can only be added to a draft test");
    }
    validate(request);

    Question question =
        questionRepository.save(
            questionMapper.toEntity(request, testId, questionRepository.getNextSortOrder()));

    List<Option> options = new ArrayList<>();
    if (request.getOptions() != null) {
      int position = 0;
      for (CreateOptionRequest optionRequest : request.getOptions()) {
        options.add(
            optionRepository.save(
                questionMapper.toOption(optionRequest, question.getId(), position++)));
      }
    }
    return questionMapper.toResponse(question, questionMapper.toOptionResponses(options));
  }

  @Override
  public TestResponse getTest(UUID testId) {
    return toTestResponse(findByIdOrThrow(testId));
  }

  @Override
  public TestListResponse listTests(UUID lessonId, Pageable pageable) {
    return mapper.toListResponse(
        lessonId == null
            ? repository.findAll(pageable)
            : repository.findByLessonId(lessonId, pageable));
  }

  private TestResponse toTestResponse(Test test) {
    List<Question> questions =
        questionRepository.findByTestId(test.getId(), QuestionRepository.BY_ORDER);
    Map<UUID, List<Option>> optionsByQuestion =
        questions.isEmpty()
            ? Map.of()
            : optionRepository
                .findByQuestionIdIn(questions.stream().map(Question::getId).toList())
                .stream()
                .sorted(Comparator.comparingInt(Option::getPosition))
                .collect(Collectors.groupingBy(Option::getQuestionId));

    List<QuestionResponse> questionResponses =
        questions.stream()
            .map(
                q ->
                    questionMapper.toResponse(
                        q,
                        questionMapper.toOptionResponses(
                            optionsByQuestion.getOrDefault(q.getId(), List.of()))))
            .toList();
    return mapper.toResponse(test, questionResponses);
  }

  private void validate(CreateQuestionRequest request) {
    QuestionType type = request.getType();
    List<CreateOptionRequest> options = request.getOptions();
    if (type == QuestionType.SHORT_TEXT || type == QuestionType.CODE) {
      if (options != null && !options.isEmpty()) {
        throw new BadRequestException("A " + type + " question takes no options");
      }
      return;
    }
    if (options == null || options.isEmpty()) {
      throw new BadRequestException("A " + type + " question requires options");
    }
    long correct = options.stream().filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
    if (correct == 0) {
      throw new BadRequestException("At least one option must be marked correct");
    }
    if ((type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_FALSE) && correct != 1) {
      throw new BadRequestException("A " + type + " question needs exactly one correct option");
    }
    if (type == QuestionType.TRUE_FALSE && options.size() != 2) {
      throw new BadRequestException("A TRUE_FALSE question needs exactly two options");
    }
  }

  private Test findByIdOrThrow(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("Test", id));
  }
}
