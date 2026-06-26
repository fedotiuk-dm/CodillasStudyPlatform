import { QuestionType } from "@/lib/api/assessment/model";

import type { TestTemplate } from "./types";

export const sqlBasics: TestTemplate = {
  id: "sql-basics",
  label: "SQL базовий",
  description: "Вибірки, фільтри, агрегати.",
  suggestedTitle: "SQL базовий",
  questions: [
    {
      type: QuestionType.SINGLE_CHOICE,
      prompt: "Який оператор вибирає всі рядки таблиці?",
      points: 1,
      options: [
        { text: "SELECT * FROM t", correct: true },
        { text: "GET * FROM t", correct: false },
        { text: "FETCH t", correct: false },
      ],
    },
    {
      type: QuestionType.TRUE_FALSE,
      prompt: "WHERE фільтрує рядки до групування.",
      points: 1,
      options: [
        { text: "Правда", correct: true },
        { text: "Хиба", correct: false },
      ],
    },
    {
      type: QuestionType.SHORT_TEXT,
      prompt: "Яка функція рахує кількість рядків?",
      points: 1,
    },
  ],
};
