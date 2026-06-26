package de.codillas.assessment.domain.model;

/**
 * Discriminator for a question's shape. Choice types carry options and auto-grade; SHORT_TEXT and
 * CODE are free-text answers graded manually by the teacher (CODE is just rendered in a code
 * editor).
 */
public enum QuestionType {
  SINGLE_CHOICE,
  MULTIPLE_CHOICE,
  TRUE_FALSE,
  SHORT_TEXT,
  CODE;

  /** Choice types are auto-gradable from their correct options; text/code answers are not. */
  public boolean isChoice() {
    return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE || this == TRUE_FALSE;
  }
}
