package de.codillas.homework.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GradeCalculator (late penalty)")
class GradeCalculatorTest {

  private final GradeCalculator calculator = new GradeCalculator();

  @Test
  @DisplayName("on time (0 days late) returns the raw score unchanged")
  void onTime() {
    assertThat(calculator.effectiveScore(80, 0, 10, 50)).isEqualTo(80);
  }

  @Test
  @DisplayName("one day late deducts one day's penalty")
  void oneDayLate() {
    // 80 * (100 - 10)/100 = 72
    assertThat(calculator.effectiveScore(80, 1, 10, 50)).isEqualTo(72);
  }

  @Test
  @DisplayName("penalty is capped at maxPenaltyPct")
  void capped() {
    // 10 days * 10% = 100%, capped at 50% -> 80 * 0.5 = 40
    assertThat(calculator.effectiveScore(80, 10, 10, 50)).isEqualTo(40);
  }

  @Test
  @DisplayName("a null maxPenaltyPct still floors the score at 0, never negative")
  void uncappedFloorsAtZero() {
    // 20 days * 10% = 200% -> clamped to 100% -> 0
    assertThat(calculator.effectiveScore(80, 20, 10, null)).isEqualTo(0);
  }

  @Test
  @DisplayName("penalty off (null pctPerDay) returns the raw score regardless of days late")
  void penaltyOff() {
    assertThat(calculator.effectiveScore(80, 3, null, null)).isEqualTo(80);
  }
}
