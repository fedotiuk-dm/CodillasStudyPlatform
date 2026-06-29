package de.codillas.gradebook.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.ProgressEntry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeightedGradeCalculator")
class WeightedGradeCalculatorTest {

  private final WeightedGradeCalculator calculator = new WeightedGradeCalculator();

  private static ProgressEntry entry(GradeSource source, int awarded, int max) {
    return ProgressEntry.builder().source(source).score(awarded).maxPoints(max).build();
  }

  @Test
  @DisplayName("sums Σawarded/Σmax across types and breaks the totals down per type")
  void weightedTotalsAndBreakdown() {
    WeightedGrade grade =
        calculator.compute(
            List.of(
                entry(GradeSource.HOMEWORK, 24, 30),
                entry(GradeSource.HOMEWORK, 10, 10),
                entry(GradeSource.TEST, 18, 20)));

    assertThat(grade.awarded()).isEqualTo(52);
    assertThat(grade.maxPoints()).isEqualTo(60);
    assertThat(grade.byType())
        .anySatisfy(
            t -> {
              assertThat(t.source()).isEqualTo(GradeSource.HOMEWORK);
              assertThat(t.awarded()).isEqualTo(34);
              assertThat(t.maxPoints()).isEqualTo(40);
            })
        .anySatisfy(
            t -> {
              assertThat(t.source()).isEqualTo(GradeSource.TEST);
              assertThat(t.awarded()).isEqualTo(18);
              assertThat(t.maxPoints()).isEqualTo(20);
            });
  }

  @Test
  @DisplayName("empty entries yield a zero grade")
  void empty() {
    WeightedGrade grade = calculator.compute(List.of());
    assertThat(grade.awarded()).isZero();
    assertThat(grade.maxPoints()).isZero();
    assertThat(grade.byType()).isEmpty();
  }
}
