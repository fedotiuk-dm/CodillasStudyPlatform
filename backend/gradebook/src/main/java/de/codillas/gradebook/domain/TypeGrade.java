package de.codillas.gradebook.domain;

import de.codillas.gradebook.domain.model.GradeSource;

/** Points-weighted grade for a single source type (homework or test). */
public record TypeGrade(GradeSource source, int awarded, int maxPoints, double percent) {}
