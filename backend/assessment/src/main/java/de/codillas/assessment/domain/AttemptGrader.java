package de.codillas.assessment.domain;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.codillas.assessment.domain.model.Answer;
import de.codillas.assessment.domain.model.Option;
import de.codillas.assessment.domain.model.Question;

/**
 * Auto-grades choice answers. A choice answer earns the question's full points only when the set of
 * selected options exactly equals the set of correct options (no partial credit). SHORT_TEXT is
 * left ungraded ({@code null}) for the teacher.
 */
@Component
public class AttemptGrader {

  /** Points for one answer, or {@code null} when the question needs manual grading. */
  public Integer autoScore(Question question, List<Option> questionOptions, Answer answer) {
    if (!question.getType().isChoice()) {
      return null;
    }
    Set<UUID> correct =
        questionOptions.stream()
            .filter(Option::isCorrect)
            .map(Option::getId)
            .collect(Collectors.toSet());
    return answer.getSelectedOptionIds().equals(correct) ? question.getPoints() : 0;
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
