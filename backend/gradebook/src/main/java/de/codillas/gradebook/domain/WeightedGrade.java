package de.codillas.gradebook.domain;

import java.util.List;

/** The final course grade: points-weighted totals Σawarded/Σmax with a per-type breakdown. */
public record WeightedGrade(int awarded, int maxPoints, List<TypeGrade> byType) {}
