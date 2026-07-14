package de.codillas.assessment.domain;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.codillas.assessment.domain.model.Answer;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;
import de.codillas.assessment.domain.model.QuestionType;

/**
 * Auto-grades choice answers. A choice answer earns the question's full points only when the set of
 * selected options exactly equals the set of correct options (no partial credit). SHORT_TEXT is
 * left ungraded ({@code null}) for the teacher.
 */
@Component
public class AttemptGrader {

  /** Points for one answer, or {@code null} when the question needs manual grading. */
  public Integer autoScore(Question question, List<Option> questionOptions, Answer answer) {
    QuestionType type = question.getType();
    if (!type.isChoice()) {
      return null;
    }
    Set<UUID> correct =
        questionOptions.stream()
            .filter(Option::isCorrect)
            .map(Option::getId)
            .collect(Collectors.toSet());
    if (type == QuestionType.MULTIPLE_CHOICE) {
      return partialScore(question.getPoints(), correct, answer.getSelectedOptionIds());
    }
    // SINGLE_CHOICE / TRUE_FALSE: exactly the correct set earns the points.
    return answer.getSelectedOptionIds().equals(correct) ? question.getPoints() : 0;
  }

  /** {@code points * max(0, (correct − incorrect) / totalCorrect)}, rounded; never below 0. */
  private int partialScore(int points, Set<UUID> correct, Set<UUID> selected) {
    if (correct.isEmpty()) {
      return 0;
    }
    long correctPicks = selected.stream().filter(correct::contains).count();
    long wrongPicks = selected.size() - correctPicks;
    double ratio = (correctPicks - wrongPicks) / (double) correct.size();
    return (int) Math.max(0, Math.round(points * ratio));
  }

  /** Sum of awarded points, treating ungraded answers as 0. */
  public int totalScore(Collection<Answer> answers) {
    return answers.stream()
        .map(Answer::getAwardedPoints)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .sum();
  }

  /** True when every answer has been graded (none pending). */
  public boolean allGraded(Collection<Answer> answers) {
    return answers.stream().allMatch(a -> a.getAwardedPoints() != null);
  }
}
