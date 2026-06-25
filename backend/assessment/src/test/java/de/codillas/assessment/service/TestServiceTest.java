package de.codillas.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.codillas.assessment.api.dto.CreateOptionRequest;
import de.codillas.assessment.api.dto.CreateQuestionRequest;
import de.codillas.assessment.api.dto.QuestionResponse;
import de.codillas.assessment.api.dto.QuestionType;
import de.codillas.assessment.domain.TestStateMachine;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.TestStatus;
import de.codillas.assessment.domain.repository.OptionRepository;
import de.codillas.assessment.domain.repository.QuestionRepository;
import de.codillas.assessment.domain.repository.TestRepository;
import de.codillas.assessment.mapper.QuestionMapper;
import de.codillas.assessment.mapper.TestMapper;
import de.codillas.shared.exception.BadRequestException;

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
}
