package de.codillas.gradebook.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.codillas.gradebook.domain.model.GradeSource;
import de.codillas.gradebook.domain.model.ProgressEntry;

/**
 * Pure final-grade math: the points-weighted percent {@code Σawarded/Σmax} over a student's
 * progress entries, with a per-source-type breakdown. No Spring/DB state — unit-tested in
 * isolation.
 */
@Component
public class WeightedGradeCalculator {

  public WeightedGrade compute(Collection<ProgressEntry> entries) {
    List<TypeGrade> byType =
        entries.stream()
            .collect(Collectors.groupingBy(ProgressEntry::getSource))
            .entrySet()
            .stream()
            .map(e -> typeGrade(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(TypeGrade::source))
            .toList();
    int awarded = byType.stream().mapToInt(TypeGrade::awarded).sum();
    int maxPoints = byType.stream().mapToInt(TypeGrade::maxPoints).sum();
    return new WeightedGrade(awarded, maxPoints, percent(awarded, maxPoints), byType);
  }

  private static TypeGrade typeGrade(GradeSource source, List<ProgressEntry> entries) {
    int awarded = entries.stream().mapToInt(ProgressEntry::getScore).sum();
    int maxPoints = entries.stream().mapToInt(ProgressEntry::getMaxPoints).sum();
    return new TypeGrade(source, awarded, maxPoints, percent(awarded, maxPoints));
  }

  /** Points-weighted percent Σawarded/Σmax, guarding division by zero. */
  private static double percent(int awarded, int maxPoints) {
    return maxPoints == 0 ? 0.0 : awarded * 100.0 / maxPoints;
  }
}
