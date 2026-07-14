"use client";

import { Trash2 } from "lucide-react";
import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { type CreateQuestionRequest, QuestionType } from "@/lib/api/assessment/model";

type ApiOption = NonNullable<CreateQuestionRequest["options"]>[number];
export type DraftOption = ApiOption & { _key: string };
export type DraftQuestion = Omit<CreateQuestionRequest, "options"> & {
  _key: string;
  options?: DraftOption[];
};

const NEEDS_OPTIONS: QuestionType[] = [
  QuestionType.SINGLE_CHOICE,
  QuestionType.MULTIPLE_CHOICE,
  QuestionType.TRUE_FALSE,
];

export function newOption(correct = false): DraftOption {
  return { _key: crypto.randomUUID(), text: "", correct };
}

export function newDraftQuestion(): DraftQuestion {
  return {
    _key: crypto.randomUUID(),
    type: QuestionType.SINGLE_CHOICE,
    prompt: "",
    points: 1,
    options: [newOption(true), newOption()],
  };
}

export function QuestionListEditor({
  value,
  onChange,
}: {
  value: DraftQuestion[];
  onChange: (next: DraftQuestion[]) => void;
}) {
  const t = useTranslations("tests");

  function patch(index: number, fields: Partial<DraftQuestion>) {
    onChange(value.map((q, i) => (i === index ? { ...q, ...fields } : q)));
  }

  function patchOption(
    qi: number,
    oi: number,
    fields: Partial<{ text: string; correct: boolean }>,
  ) {
    const q = value[qi];
    const options = (q.options ?? []).map((o, i) => (i === oi ? { ...o, ...fields } : o));
    patch(qi, { options });
  }

  function move(index: number, delta: number) {
    const next = [...value];
    const target = index + delta;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  return (
    <div className="grid gap-4">
      {value.map((q, qi) => {
        const needsOptions = NEEDS_OPTIONS.includes(q.type);
        return (
          <div key={q._key} className="grid gap-3 rounded-md border p-3">
            <div className="flex items-center justify-between">
              <span className="font-medium text-sm">{t("questionNumber", { number: qi + 1 })}</span>
              <div className="flex items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => move(qi, -1)}
                  disabled={qi === 0}
                >
                  {t("moveUp")}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => move(qi, 1)}
                  disabled={qi === value.length - 1}
                >
                  {t("moveDown")}
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={t("removeQuestion")}
                  onClick={() => onChange(value.filter((_, i) => i !== qi))}
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            </div>

            <Select
              value={q.type}
              onValueChange={(v) => {
                const type = v as QuestionType;
                const becomesOptions = NEEDS_OPTIONS.includes(type);
                patch(qi, {
                  type,
                  options: becomesOptions
                    ? q.options?.length
                      ? q.options
                      : [newOption(true), newOption()]
                    : undefined,
                });
              }}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.values(QuestionType).map((qt) => (
                  <SelectItem key={qt} value={qt}>
                    {qt}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Input
              placeholder={t("promptPlaceholder")}
              value={q.prompt}
              onChange={(e) => patch(qi, { prompt: e.target.value })}
            />

            <Input
              type="number"
              min={1}
              value={q.points}
              onChange={(e) => patch(qi, { points: Number(e.target.value) })}
            />

            {needsOptions && (
              <div className="grid gap-2">
                {(q.options ?? []).map((o, oi) => (
                  <div key={o._key} className="flex items-center gap-2">
                    <Checkbox
                      checked={o.correct}
                      onCheckedChange={(c) => patchOption(qi, oi, { correct: c === true })}
                    />
                    <Input
                      className="flex-1"
                      placeholder={t("optionPlaceholder", { number: oi + 1 })}
                      value={o.text}
                      onChange={(e) => patchOption(qi, oi, { text: e.target.value })}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() =>
                        patch(qi, { options: (q.options ?? []).filter((_, i) => i !== oi) })
                      }
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="justify-self-start"
                  onClick={() => patch(qi, { options: [...(q.options ?? []), newOption()] })}
                >
                  {t("addOption")}
                </Button>
              </div>
            )}
          </div>
        );
      })}

      <Separator />
      <Button
        type="button"
        variant="outline"
        className="justify-self-start"
        onClick={() => onChange([...value, newDraftQuestion()])}
      >
        {t("addQuestion")}
      </Button>
    </div>
  );
}
