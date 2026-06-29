package de.codillas.gradebook.domain;

import java.util.List;

/** The final course grade: points-weighted percent Σawarded/Σmax with a per-type breakdown. */
public record WeightedGrade(int awarded, int maxPoints, List<TypeGrade> byType) {

  public double percent() {
    return maxPoints == 0 ? 0.0 : awarded * 100.0 / maxPoints;
  }
}
