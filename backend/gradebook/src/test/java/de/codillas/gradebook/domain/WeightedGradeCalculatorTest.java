package de.codillas.gradebook.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
  @DisplayName("weights Σawarded/Σmax across types and breaks the percent down per type")
  void weightedPercentAndBreakdown() {
    WeightedGrade grade =
        calculator.compute(
            List.of(
                entry(GradeSource.HOMEWORK, 24, 30),
                entry(GradeSource.HOMEWORK, 10, 10),
                entry(GradeSource.TEST, 18, 20)));

    assertThat(grade.awarded()).isEqualTo(52);
    assertThat(grade.maxPoints()).isEqualTo(60);
    assertThat(grade.percent()).isCloseTo(86.667, within(0.01));
    assertThat(grade.byType())
        .anySatisfy(
            t -> {
              assertThat(t.source()).isEqualTo(GradeSource.HOMEWORK);
              assertThat(t.awarded()).isEqualTo(34);
              assertThat(t.maxPoints()).isEqualTo(40);
              assertThat(t.percent()).isCloseTo(85.0, within(0.01));
            })
        .anySatisfy(
            t -> {
              assertThat(t.source()).isEqualTo(GradeSource.TEST);
              assertThat(t.percent()).isCloseTo(90.0, within(0.01));
            });
  }

  @Test
  @DisplayName("empty entries yield a zero grade with no division by zero")
  void empty() {
    WeightedGrade grade = calculator.compute(List.of());
    assertThat(grade.awarded()).isZero();
    assertThat(grade.maxPoints()).isZero();
    assertThat(grade.percent()).isZero();
    assertThat(grade.byType()).isEmpty();
  }
}
