"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { WizardShell } from "@/components/shared/wizard-shell";
import { Card, CardContent } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  useAddQuestion,
  useCreateTest,
  usePublishTest,
} from "@/lib/api/assessment/assessment/assessment";
import { type DraftQuestion, QuestionListEditor } from "./question-list-editor";
import { getTestTemplates, type TestTemplate } from "./templates";

type Step = 0 | 1 | 2;

function toRequests(questions: DraftQuestion[]) {
  return questions.map(({ _key, ...q }) => ({
    ...q,
    options: q.options?.filter((o) => o.text.trim()),
  }));
}

export function TestWizard({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("tests");
  const createTest = useCreateTest();
  const addQuestion = useAddQuestion();
  const publish = usePublishTest();

  const [step, setStep] = useState<Step>(0);
  const [title, setTitle] = useState("");
  const [questions, setQuestions] = useState<DraftQuestion[]>([]);
  const [publishNow, setPublishNow] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function reset() {
    setStep(0);
    setTitle("");
    setQuestions([]);
    setPublishNow(false);
    setSubmitting(false);
  }

  function applyTemplate(tpl: TestTemplate | null) {
    if (!tpl) {
      setQuestions([]);
      setStep(1);
      return;
    }
    if (!title.trim()) setTitle(tpl.suggestedTitle);
    setQuestions(tpl.questions.map((q) => ({ ...q, _key: crypto.randomUUID() })));
    setStep(1);
  }

  const totalPoints = questions.reduce((sum, q) => sum + (Number(q.points) || 0), 0);
  const canAdvance =
    step === 0 ? title.trim().length > 0 : step === 1 ? questions.length > 0 : true;

  async function onCreate() {
    if (questions.length === 0) {
      toast.error(t("noQuestions"));
      return;
    }
    setSubmitting(true);
    try {
      const test = await createTest.mutateAsync({ data: { title: title.trim() } });
      const requests = toRequests(questions);
      for (let i = 0; i < requests.length; i++) {
        try {
          await addQuestion.mutateAsync({ testId: test.id, data: requests[i] });
        } catch {
          toast.error(t("questionFailed", { number: i + 1 }));
          onOpenChange(false);
          reset();
          return;
        }
      }
      if (publishNow) await publish.mutateAsync({ testId: test.id });
      toast.success(t("created"));
      onOpenChange(false);
      reset();
    } finally {
      setSubmitting(false);
    }
  }

  function handleOpenChange(next: boolean) {
    if (!next) reset();
    onOpenChange(next);
  }

  return (
    <WizardShell
      open={open}
      onOpenChange={handleOpenChange}
      title={t("newTest")}
      description={t("dialogDescription")}
      stepCount={3}
      activeStep={step}
      canAdvance={canAdvance}
      isSubmitting={submitting}
      onBack={() => setStep((s) => Math.max(0, s - 1) as Step)}
      onNext={() => setStep((s) => Math.min(2, s + 1) as Step)}
      onSubmit={onCreate}
      backLabel={t("wizardBack")}
      nextLabel={t("wizardNext")}
      submitLabel={t("createTest")}
      submittingLabel={t("creating")}
    >
      {step === 0 && (
        <div className="grid gap-4">
          <div className="grid gap-1.5">
            <Label htmlFor="test-title">{t("titleLabel")}</Label>
            <Input
              id="test-title"
              placeholder={t("titlePlaceholder")}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>
          <p className="text-muted-foreground text-sm">{t("useTemplate")}</p>
          <div className="grid gap-2 sm:grid-cols-2">
            {getTestTemplates().map((tpl) => (
              <Card
                key={tpl.id}
                className="cursor-pointer transition-colors hover:border-primary"
                onClick={() => applyTemplate(tpl)}
              >
                <CardContent className="grid gap-1 p-4">
                  <span className="font-medium">{tpl.label}</span>
                  <span className="text-muted-foreground text-xs">{tpl.description}</span>
                  <span className="text-muted-foreground text-xs">
                    {t("templateQuestions", { count: tpl.questions.length })}
                  </span>
                </CardContent>
              </Card>
            ))}
            <Card
              className="cursor-pointer transition-colors hover:border-primary"
              onClick={() => applyTemplate(null)}
            >
              <CardContent className="flex h-full items-center p-4 font-medium">
                {t("blankTest")}
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {step === 1 && <QuestionListEditor value={questions} onChange={setQuestions} />}

      {step === 2 && (
        <div className="grid gap-3">
          <div className="flex items-center justify-between">
            <span className="font-medium">{title}</span>
            <span className="text-muted-foreground text-sm">
              {t("templateQuestions", { count: questions.length })} · {t("totalPoints")}:{" "}
              {totalPoints}
            </span>
          </div>
          <ol className="grid gap-1.5 text-sm">
            {questions.map((q, i) => (
              <li key={q._key} className="flex gap-2">
                <span className="text-muted-foreground">{i + 1}.</span>
                <span className="flex-1">{q.prompt || "—"}</span>
                <span className="text-muted-foreground">{q.type}</span>
              </li>
            ))}
          </ol>
          <label htmlFor="publish-now" className="flex items-center gap-2 text-sm">
            <Checkbox
              id="publish-now"
              checked={publishNow}
              onCheckedChange={(c) => setPublishNow(c === true)}
            />
            {t("publishNow")}
          </label>
        </div>
      )}
    </WizardShell>
  );
}
