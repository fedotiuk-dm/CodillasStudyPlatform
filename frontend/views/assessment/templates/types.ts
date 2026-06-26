import type { CreateQuestionRequest } from "@/lib/api/assessment/model";

export type TestTemplate = {
  id: string;
  label: string;
  description: string;
  suggestedTitle: string;
  questions: CreateQuestionRequest[];
};
