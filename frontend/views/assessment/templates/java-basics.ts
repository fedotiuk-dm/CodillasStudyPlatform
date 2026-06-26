import { QuestionType } from "@/lib/api/assessment/model";

import type { TestTemplate } from "./types";

export const javaBasics: TestTemplate = {
  id: "java-basics",
  label: "Java основи",
  description: "10 питань: типи, JVM, синтаксис.",
  suggestedTitle: "Java основи",
  questions: [
    {
      type: QuestionType.SINGLE_CHOICE,
      prompt: "Що таке JVM?",
      points: 1,
      options: [
        { text: "Віртуальна машина, що виконує байткод", correct: true },
        { text: "Компілятор Java у машинний код", correct: false },
        { text: "Менеджер пакетів", correct: false },
      ],
    },
    {
      type: QuestionType.TRUE_FALSE,
      prompt: "Примітив int може містити значення null.",
      points: 1,
      options: [
        { text: "Правда", correct: false },
        { text: "Хиба", correct: true },
      ],
    },
    {
      type: QuestionType.MULTIPLE_CHOICE,
      prompt: "Які з наведеного — примітивні типи Java?",
      points: 2,
      options: [
        { text: "int", correct: true },
        { text: "boolean", correct: true },
        { text: "String", correct: false },
        { text: "double", correct: true },
      ],
    },
    {
      type: QuestionType.SHORT_TEXT,
      prompt: "Яким ключовим словом оголошують константу?",
      points: 1,
    },
    {
      type: QuestionType.CODE,
      prompt: "Напишіть метод, що повертає суму двох int.",
      points: 3,
    },
  ],
};
