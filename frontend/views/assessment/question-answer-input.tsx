"use client";

import { useTranslations } from "next-intl";

import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import type { QuestionResponse } from "@/lib/api/assessment/model";

export type AnswerDraft = { selectedOptionIds: string[]; text: string };

export function QuestionAnswerInput({
  index,
  question: q,
  draft,
  onChange,
}: {
  index: number;
  question: QuestionResponse;
  draft?: AnswerDraft;
  onChange: (patch: Partial<AnswerDraft>) => void;
}) {
  const t = useTranslations("tests");
  const selected = draft?.selectedOptionIds ?? [];
  const single = q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE";

  return (
    <div className="grid gap-2 rounded-md border p-3">
      <p className="font-medium text-sm">
        {index + 1}. {q.prompt}{" "}
        <span className="font-normal text-muted-foreground">
          ({q.points} {t("pt")})
        </span>
      </p>
      {q.type === "CODE" ? (
        <Textarea
          className="font-mono text-sm"
          rows={8}
          spellCheck={false}
          placeholder={t("codePlaceholder")}
          value={draft?.text ?? ""}
          onChange={(e) => onChange({ text: e.target.value })}
        />
      ) : q.type === "SHORT_TEXT" ? (
        <Input
          placeholder={t("answerPlaceholder")}
          value={draft?.text ?? ""}
          onChange={(e) => onChange({ text: e.target.value })}
        />
      ) : (
        <div className="grid gap-1.5">
          {q.options.map((o) =>
            single ? (
              <label key={o.id} className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name={`q-${q.id}`}
                  checked={selected[0] === o.id}
                  onChange={() => onChange({ selectedOptionIds: [o.id] })}
                />
                {o.text}
              </label>
            ) : (
              <label
                key={o.id}
                htmlFor={`q-${q.id}-${o.id}`}
                className="flex items-center gap-2 text-sm"
              >
                <Checkbox
                  id={`q-${q.id}-${o.id}`}
                  checked={selected.includes(o.id)}
                  onCheckedChange={(c) =>
                    onChange({
                      selectedOptionIds: c
                        ? [...selected, o.id]
                        : selected.filter((id) => id !== o.id),
                    })
                  }
                />
                {o.text}
              </label>
            ),
          )}
        </div>
      )}
    </div>
  );
}
