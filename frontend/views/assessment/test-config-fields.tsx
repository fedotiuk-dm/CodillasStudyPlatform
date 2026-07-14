"use client";

import { useTranslations } from "next-intl";

import { DateTimeField } from "@/components/shared/date-time-field";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

// Optional delivery rules for a test. Numbers are omitted (undefined) when blank so the request
// stays sparse; toggles are always concrete booleans (the server stores them non-null).
export type TestConfig = {
  maxAttempts?: number;
  durationMinutes?: number;
  availableFrom?: string;
  availableUntil?: string;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
};

export const EMPTY_TEST_CONFIG: TestConfig = {
  shuffleQuestions: false,
  shuffleOptions: false,
};

// Empty → undefined; otherwise a whole number clamped to ≥ 1 (matches the spec's @minimum 1).
function positiveInt(raw: string): number | undefined {
  if (!raw.trim()) return undefined;
  const n = Math.floor(Number(raw));
  return Number.isFinite(n) && n >= 1 ? n : undefined;
}

export function TestConfigFields({
  value,
  onChange,
}: {
  value: TestConfig;
  onChange: (patch: Partial<TestConfig>) => void;
}) {
  const t = useTranslations("tests");

  return (
    <div className="grid gap-4">
      <p className="font-medium text-sm">{t("configTitle")}</p>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid gap-1.5">
          <Label htmlFor="cfg-max-attempts">{t("maxAttemptsLabel")}</Label>
          <Input
            id="cfg-max-attempts"
            type="number"
            min={1}
            placeholder={t("unlimited")}
            value={value.maxAttempts ?? ""}
            onChange={(e) => onChange({ maxAttempts: positiveInt(e.target.value) })}
          />
        </div>

        <div className="grid gap-1.5">
          <Label htmlFor="cfg-duration">{t("durationLabel")}</Label>
          <Input
            id="cfg-duration"
            type="number"
            min={1}
            placeholder={t("untimed")}
            value={value.durationMinutes ?? ""}
            onChange={(e) => onChange({ durationMinutes: positiveInt(e.target.value) })}
          />
        </div>

        <div className="grid gap-1.5">
          <Label htmlFor="cfg-from">{t("availableFromLabel")}</Label>
          <DateTimeField
            id="cfg-from"
            mode="datetime"
            placeholder={t("anytime")}
            value={value.availableFrom}
            onChange={(v) => onChange({ availableFrom: v })}
          />
        </div>

        <div className="grid gap-1.5">
          <Label htmlFor="cfg-until">{t("availableUntilLabel")}</Label>
          <DateTimeField
            id="cfg-until"
            mode="datetime"
            placeholder={t("anytime")}
            value={value.availableUntil}
            onChange={(v) => onChange({ availableUntil: v })}
          />
        </div>
      </div>

      <label htmlFor="cfg-shuffle-q" className="flex items-center gap-2 text-sm">
        <Checkbox
          id="cfg-shuffle-q"
          checked={value.shuffleQuestions}
          onCheckedChange={(c) => onChange({ shuffleQuestions: c === true })}
        />
        {t("shuffleQuestions")}
      </label>

      <label htmlFor="cfg-shuffle-o" className="flex items-center gap-2 text-sm">
        <Checkbox
          id="cfg-shuffle-o"
          checked={value.shuffleOptions}
          onCheckedChange={(c) => onChange({ shuffleOptions: c === true })}
        />
        {t("shuffleOptions")}
      </label>
    </div>
  );
}
