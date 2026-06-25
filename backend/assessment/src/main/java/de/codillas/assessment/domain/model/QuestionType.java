package de.codillas.assessment.domain.model;

/**
 * Discriminator for a question's shape. Choice types carry options; SHORT_TEXT is graded manually.
 */
public enum QuestionType {
  SINGLE_CHOICE,
  MULTIPLE_CHOICE,
  TRUE_FALSE,
  SHORT_TEXT;

  /** Choice types are auto-gradable from their correct options. */
  public boolean isChoice() {
    return this != SHORT_TEXT;
  }
}
