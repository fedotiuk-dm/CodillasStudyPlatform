import { QuestionType } from "@/lib/api/assessment/model";

import type { TestTemplate } from "./types";

export const htmlCssIntro: TestTemplate = {
  id: "html-css-intro",
  label: "HTML/CSS вступ",
  description: "Теги, селектори, блокова модель.",
  suggestedTitle: "HTML/CSS вступ",
  questions: [
    {
      type: QuestionType.SINGLE_CHOICE,
      prompt: "Який тег створює гіперпосилання?",
      points: 1,
      options: [
        { text: "<a>", correct: true },
        { text: "<link>", correct: false },
        { text: "<href>", correct: false },
      ],
    },
    {
      type: QuestionType.MULTIPLE_CHOICE,
      prompt: "Які з наведеного — валідні CSS-селектори?",
      points: 2,
      options: [
        { text: ".class", correct: true },
        { text: "#id", correct: true },
        { text: "$name", correct: false },
      ],
    },
    {
      type: QuestionType.SHORT_TEXT,
      prompt: "Яка властивість задає зовнішній відступ?",
      points: 1,
    },
  ],
};
