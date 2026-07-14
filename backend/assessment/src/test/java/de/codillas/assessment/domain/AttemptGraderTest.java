package de.codillas.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.codillas.assessment.domain.model.Answer;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.QuestionType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AttemptGrader")
class AttemptGraderTest {

  private final AttemptGrader grader = new AttemptGrader();

  private static final UUID A = UUID.randomUUID();
  private static final UUID B = UUID.randomUUID();
  private static final UUID C = UUID.randomUUID();

  private static Option option(UUID id, boolean correct) {
    return Option.builder().id(id).correct(correct).build();
  }

  private static Question question(QuestionType type, int points) {
    return Question.builder().type(type).points(points).build();
  }

  private static Answer answer(Set<UUID> selected) {
    return Answer.builder().selectedOptionIds(selected).build();
  }

  @Test
  @DisplayName("single choice: full points only for the exact correct option")
  void singleChoice() {
    List<Option> options = List.of(option(A, true), option(B, false));
    assertThat(
            grader.autoScore(question(QuestionType.SINGLE_CHOICE, 5), options, answer(Set.of(A))))
        .isEqualTo(5);
    assertThat(
            grader.autoScore(question(QuestionType.SINGLE_CHOICE, 5), options, answer(Set.of(B))))
        .isZero();
  }

  @Test
  @DisplayName(
      "multiple choice: proportional partial credit, penalises wrong picks, never below zero")
  void multipleChoice_partialCredit() {
    // 3 correct (A, C, D), 2 incorrect (B, E); points = 6 → each correct worth 2.
    UUID D = UUID.randomUUID();
    UUID E = UUID.randomUUID();
    List<Option> options =
        List.of(
            option(A, true), option(B, false), option(C, true), option(D, true), option(E, false));
    Question q = question(QuestionType.MULTIPLE_CHOICE, 6);

    assertThat(grader.autoScore(q, options, answer(Set.of(A, C, D)))).isEqualTo(6); // all correct
    assertThat(grader.autoScore(q, options, answer(Set.of(A, C)))).isEqualTo(4); // partial 2/3
    assertThat(grader.autoScore(q, options, answer(Set.of(A)))).isEqualTo(2); // partial 1/3
    assertThat(grader.autoScore(q, options, answer(Set.of()))).isZero(); // empty selection
    assertThat(grader.autoScore(q, options, answer(Set.of(B, E)))).isZero(); // all wrong → clamp
    assertThat(grader.autoScore(q, options, answer(Set.of(A, C, D, B, E)))).isEqualTo(2); // (3-2)/3
    assertThat(grader.autoScore(q, options, answer(Set.of(A, B)))).isZero(); // (1-1)/3 → 0
  }

  @Test
  @DisplayName("short text is not auto-graded (null)")
  void shortText_isNull() {
    assertThat(grader.autoScore(question(QuestionType.SHORT_TEXT, 3), List.of(), answer(Set.of())))
        .isNull();
  }

  @Test
  @DisplayName("code is not auto-graded (null) — manual like short text")
  void code_isNull() {
    assertThat(grader.autoScore(question(QuestionType.CODE, 10), List.of(), answer(Set.of())))
        .isNull();
  }

  @Test
  @DisplayName("totalScore sums graded answers and ignores pending ones")
  void totalScore_sumsGraded() {
    Answer graded = Answer.builder().awardedPoints(5).build();
    Answer pending = Answer.builder().build();
    Answer zero = Answer.builder().awardedPoints(0).build();
    assertThat(grader.totalScore(List.of(graded, pending, zero))).isEqualTo(5);
  }

  @Test
  @DisplayName("allGraded is false while any answer is pending")
  void allGraded() {
    assertThat(grader.allGraded(List.of(Answer.builder().awardedPoints(1).build()))).isTrue();
    assertThat(
            grader.allGraded(
                List.of(Answer.builder().awardedPoints(1).build(), Answer.builder().build())))
        .isFalse();
  }
}
