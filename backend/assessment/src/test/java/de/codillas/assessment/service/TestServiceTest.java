package de.codillas.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import de.codillas.assessment.api.dto.CreateOptionRequest;
import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.OptionResponse;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.QuestionType;
import de.codillas.assessment.api.dto.TestResponse;
import de.codillas.assessment.domain.TestStateMachine;
import de.codillas.assessment.domain.model.Attempt;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.assessment.domain.repository.AttemptRepository;
import de.codillas.assessment.domain.repository.OptionRepository;
import de.codillas.assessment.domain.repository.QuestionRepository;
import de.codillas.assessment.domain.repository.TestRepository;
import de.codillas.assessment.mapper.QuestionMapper;
import de.codillas.assessment.mapper.QuestionMapperImpl;
import de.codillas.assessment.mapper.TestMapper;
import de.codillas.assessment.mapper.TestMapperImpl;
import de.codillas.shared.exception.BadRequestException;
import de.codillas.shared.security.CurrentUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestService")
class TestServiceTest {

  @Mock private TestRepository repository;
  @Mock private QuestionRepository questionRepository;
  @Mock private OptionRepository optionRepository;
  @Mock private TestMapper mapper;
  @Mock private QuestionMapper questionMapper;
  @Mock private TestStateMachine stateMachine;
  @Mock private CurrentUser currentUser;
  @Mock private AttemptRepository attemptRepository;
  @InjectMocks private TestServiceImpl service;

  private static CreateOptionRequest option(boolean correct) {
    CreateOptionRequest o = mock(CreateOptionRequest.class);
    lenient().when(o.getCorrect()).thenReturn(correct);
    return o;
  }

  private CreateQuestionRequest request(QuestionType type, List<CreateOptionRequest> options) {
    CreateQuestionRequest r = mock(CreateQuestionRequest.class);
    lenient().when(r.getType()).thenReturn(type);
    if (options != null) {
      lenient().when(r.getOptions()).thenReturn(options);
    }
    return r;
  }

  private void draftTest(UUID testId) {
    when(repository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.DRAFT)
                    .build()));
  }

  @Test
  @DisplayName("addQuestion persists a single-choice question with its options")
  void addQuestion_singleChoice_ok() {
    UUID testId = UUID.randomUUID();
    draftTest(testId);
    CreateQuestionRequest request =
        request(QuestionType.SINGLE_CHOICE, List.of(option(true), option(false)));
    Question question = Question.builder().id(UUID.randomUUID()).build();
    QuestionResponse dto = mock(QuestionResponse.class);
    when(questionMapper.toEntity(eq(request), eq(testId), anyInt())).thenReturn(question);
    when(questionRepository.save(question)).thenReturn(question);
    when(questionMapper.toOption(any(), eq(question.getId()), anyInt()))
        .thenReturn(Option.builder().build());
    when(optionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(questionMapper.toResponse(eq(question), any())).thenReturn(dto);

    assertThat(service.addQuestion(testId, request)).isSameAs(dto);
  }

  @Test
  @DisplayName("addQuestion rejects options on a SHORT_TEXT question")
  void addQuestion_textWithOptions_rejected() {
    UUID testId = UUID.randomUUID();
    draftTest(testId);
    CreateQuestionRequest request = request(QuestionType.SHORT_TEXT, List.of(option(true)));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.addQuestion(testId, request));
  }

  @Test
  @DisplayName("addQuestion rejects a single-choice question with more than one correct option")
  void addQuestion_singleChoiceTwoCorrect_rejected() {
    UUID testId = UUID.randomUUID();
    draftTest(testId);
    CreateQuestionRequest request =
        request(QuestionType.SINGLE_CHOICE, List.of(option(true), option(true)));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.addQuestion(testId, request));
  }

  @Test
  @DisplayName("addQuestion rejects questions on a non-draft test")
  void addQuestion_published_rejected() {
    UUID testId = UUID.randomUUID();
    when(repository.findById(testId))
        .thenReturn(
            Optional.of(
                de.codillas.assessment.domain.model.Test.builder()
                    .status(TestStatus.PUBLISHED)
                    .build()));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> service.addQuestion(testId, request(QuestionType.SHORT_TEXT, null)));
  }

  @Test
  @DisplayName("getTest returns builder order when the caller has no attempt")
  void getTest_noAttempt_builderOrder() {
    UUID testId = UUID.randomUUID();
    de.codillas.assessment.domain.model.Test test =
        de.codillas.assessment.domain.model.Test.builder()
            .id(testId)
            .title("t")
            .shuffleQuestions(true)
            .build();
    when(repository.findById(testId)).thenReturn(Optional.of(test));
    when(currentUser.id()).thenReturn(UUID.randomUUID());
    when(attemptRepository.findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(any(), any()))
        .thenReturn(Optional.empty());
    when(questionRepository.findByTestId(eq(testId), any())).thenReturn(List.of());
    when(mapper.toResponse(eq(test), any())).thenReturn(mock(TestResponse.class));

    assertThat(service.getTest(testId)).isNotNull();
  }

  /**
   * Exercises the real taker path (getTest) through the REAL MapStruct mappers — not a mock — with
   * shuffleQuestions/shuffleOptions on and real questions+options. Verifies (a) the order a student
   * sees is a deterministic function of the attemptId (same attemptId -> identical order across two
   * independent calls) and is genuinely shuffled away from builder order, and (b) the taker DTO
   * never carries the server-only {@code correct} flag.
   */
  @Test
  @DisplayName("taker DTO shuffle is attemptId-deterministic and leaks no correct flag")
  void getTest_takerShuffle_deterministicAndNoLeak() {
    // Real mappers, mock repositories: the true taker-facing serialization path.
    TestServiceImpl realService =
        new TestServiceImpl(
            repository,
            questionRepository,
            optionRepository,
            new TestMapperImpl(),
            new QuestionMapperImpl(),
            stateMachine,
            currentUser,
            attemptRepository);

    UUID testId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    // Fixed attempt id == fixed shuffle seed, so the order must be reproducible.
    UUID attemptId = UUID.fromString("11111111-2222-3333-4444-555555555555");

    de.codillas.assessment.domain.model.Test test =
        de.codillas.assessment.domain.model.Test.builder()
            .id(testId)
            .title("taker view")
            .shuffleQuestions(true)
            .shuffleOptions(true)
            .build();

    // 8 questions, each with 4 options (exactly one correct) in builder/position order.
    List<Question> questions = new ArrayList<>();
    List<Option> allOptions = new ArrayList<>();
    Map<UUID, List<UUID>> builderOptionOrder = new java.util.LinkedHashMap<>();
    for (int q = 0; q < 8; q++) {
      UUID questionId = UUID.randomUUID();
      questions.add(
          Question.builder()
              .id(questionId)
              .testId(testId)
              .type(de.codillas.assessment.domain.model.QuestionType.SINGLE_CHOICE)
              .prompt("Q" + q)
              .points(1)
              .sortOrder(q)
              .build());
      List<UUID> optIds = new ArrayList<>();
      for (int o = 0; o < 4; o++) {
        UUID optId = UUID.randomUUID();
        optIds.add(optId);
        allOptions.add(
            Option.builder()
                .id(optId)
                .questionId(questionId)
                .text("Q" + q + "-opt" + o)
                .correct(o == 0) // first option is the correct one — must never surface
                .position(o)
                .build());
      }
      builderOptionOrder.put(questionId, optIds);
    }
    List<UUID> builderQuestionOrder = questions.stream().map(Question::getId).toList();

    when(repository.findById(testId)).thenReturn(Optional.of(test));
    when(currentUser.id()).thenReturn(studentId);
    when(attemptRepository.findFirstByTestIdAndStudentIdOrderByAttemptNumberDesc(testId, studentId))
        .thenReturn(Optional.of(Attempt.builder().id(attemptId).build()));
    when(questionRepository.findByTestId(eq(testId), any())).thenReturn(questions);
    when(optionRepository.findByQuestionIdIn(any())).thenReturn(allOptions);

    TestResponse first = realService.getTest(testId);
    TestResponse second = realService.getTest(testId);

    // The real mapper actually ran: DTO is populated, not a stub.
    assertThat(first.getQuestions()).hasSize(8);
    assertThat(first.getQuestions().get(0).getOptions()).hasSize(4);
    assertThat(first.getQuestions().get(0).getOptions().get(0).getText()).isNotBlank();

    List<UUID> takerQuestionOrder =
        first.getQuestions().stream().map(QuestionResponse::getId).toList();

    // (a1) Deterministic across two independent calls with the same attemptId.
    assertThat(takerQuestionOrder)
        .isEqualTo(second.getQuestions().stream().map(QuestionResponse::getId).toList());
    // (a2) Genuinely shuffled — not just builder order returned.
    assertThat(takerQuestionOrder)
        .containsExactlyInAnyOrderElementsOf(builderQuestionOrder)
        .isNotEqualTo(builderQuestionOrder);

    // Options: deterministic per question, and at least one question's options are reordered.
    Map<UUID, List<UUID>> takerOptionOrder = optionOrderByQuestion(first);
    assertThat(takerOptionOrder).isEqualTo(optionOrderByQuestion(second));
    boolean anyOptionsReordered =
        takerOptionOrder.entrySet().stream()
            .anyMatch(e -> !e.getValue().equals(builderOptionOrder.get(e.getKey())));
    assertThat(anyOptionsReordered)
        .as("shuffleOptions must reorder at least one question's options")
        .isTrue();
    // Each question still carries the full option multiset.
    takerOptionOrder.forEach(
        (qid, opts) ->
            assertThat(opts).containsExactlyInAnyOrderElementsOf(builderOptionOrder.get(qid)));

    // (b) No-leak: the taker option DTO exposes no correctness accessor whatsoever.
    for (Method m : OptionResponse.class.getDeclaredMethods()) {
      if (m.getParameterCount() == 0
          && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
        assertThat(m.getName().toLowerCase())
            .as("taker option DTO must not expose the correct flag")
            .doesNotContain("correct");
      }
    }
  }

  private static Map<UUID, List<UUID>> optionOrderByQuestion(TestResponse response) {
    return response.getQuestions().stream()
        .collect(
            Collectors.toMap(
                QuestionResponse::getId,
                q -> q.getOptions().stream().map(OptionResponse::getId).toList()));
  }

  @Test
  @DisplayName("listTests hides drafts from students")
  void listTests_studentSeesPublishedOnly() {
    org.springframework.data.domain.Pageable pageable =
        org.springframework.data.domain.Pageable.unpaged();
    de.codillas.assessment.api.dto.TestListResponse dto =
        mock(de.codillas.assessment.api.dto.TestListResponse.class);
    when(currentUser.isStaff()).thenReturn(false);
    when(repository.findByStatus(TestStatus.PUBLISHED, pageable))
        .thenReturn(org.springframework.data.domain.Page.empty());
    when(mapper.toListResponse(org.springframework.data.domain.Page.empty())).thenReturn(dto);

    assertThat(service.listTests(null, pageable)).isSameAs(dto);
  }
}
