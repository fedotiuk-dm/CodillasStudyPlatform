package de.codillas.gradebook.domain;

import java.util.List;

/** The final course grade: points-weighted percent Σawarded/Σmax with a per-type breakdown. */
public record WeightedGrade(int awarded, int maxPoints, double percent, List<TypeGrade> byType) {}
