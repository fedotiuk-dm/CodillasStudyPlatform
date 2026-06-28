package de.codillas.homework.domain;

import org.springframework.stereotype.Component;

import org.jspecify.annotations.Nullable;

/**
 * Pure late-penalty math: turns a raw graded score into the effective score after a per-day
 * deduction, capped. {@code penaltyPctPerDay == null} means the assignment has no late penalty.
 */
@Component
public class GradeCalculator {

  public int effectiveScore(
      int rawScore,
      long daysLate,
      @Nullable Integer penaltyPctPerDay,
      @Nullable Integer maxPenaltyPct) {
    if (penaltyPctPerDay == null || daysLate <= 0) {
      return rawScore;
    }
    long penaltyPct = penaltyPctPerDay * daysLate;
    if (maxPenaltyPct != null) {
      penaltyPct = Math.min(penaltyPct, maxPenaltyPct);
    }
    penaltyPct = Math.min(penaltyPct, 100);
    int effective = (int) Math.round(rawScore * (100 - penaltyPct) / 100.0);
    return Math.max(0, effective);
  }
}
