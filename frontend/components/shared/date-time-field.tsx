"use client";

import { format } from "date-fns";
import { CalendarIcon, ChevronDown } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";

// 24-hour time, picked from dropdowns. Minutes step by 5 — bump to step 1 if exact minutes matter.
const HOURS = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, "0"));
const MINUTES = Array.from({ length: 12 }, (_, i) => String(i * 5).padStart(2, "0"));

/**
 * The single date / date-time picker for the whole app. Calendar popover + optional time input.
 *
 * Value is always a string the API accepts directly, or `undefined` when empty:
 *   - mode="date":     "yyyy-MM-dd"            (zod.iso.date)
 *   - mode="datetime": ISO 8601 with offset    (zod.iso.datetime, e.g. "2026-06-26T12:30:00.000Z")
 *
 * Drop it straight into react-hook-form: `value={field.value}` `onChange={field.onChange}`.
 * Empty → `undefined`, so optional fields validate and the form can submit.
 */
export function DateTimeField({
  value,
  onChange,
  mode = "date",
  placeholder,
  disabled,
  id,
}: {
  value?: string;
  onChange: (value: string | undefined) => void;
  mode?: "date" | "datetime";
  placeholder?: string;
  disabled?: boolean;
  id?: string;
}) {
  const parsed = value ? new Date(value) : undefined;
  const selected = parsed && !Number.isNaN(parsed.getTime()) ? parsed : undefined;
  const hh = selected ? format(selected, "HH") : "00";
  const mm = selected ? format(selected, "mm") : "00";

  function commit(date: Date | undefined, hour: string, minute: string) {
    if (!date) return onChange(undefined);
    if (mode === "date") return onChange(format(date, "yyyy-MM-dd"));
    const next = new Date(date);
    next.setHours(Number(hour), Number(minute), 0, 0);
    onChange(next.toISOString());
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          disabled={disabled}
          className={cn(
            "w-full justify-start text-left font-normal",
            !selected && "text-muted-foreground",
          )}
        >
          <CalendarIcon className="size-4" />
          {selected
            ? format(selected, mode === "datetime" ? "PPP, HH:mm" : "PPP")
            : (placeholder ?? "Pick a date")}
          <ChevronDown className="ml-auto size-4 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0" align="start">
        <Calendar mode="single" selected={selected} onSelect={(d) => commit(d, hh, mm)} autoFocus />
        {mode === "datetime" && (
          <div className="flex items-center gap-2 border-t p-3">
            <Select disabled={!selected} value={hh} onValueChange={(h) => commit(selected, h, mm)}>
              <SelectTrigger className="w-[72px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {HOURS.map((h) => (
                  <SelectItem key={h} value={h}>
                    {h}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <span className="text-muted-foreground">:</span>
            <Select disabled={!selected} value={mm} onValueChange={(m) => commit(selected, hh, m)}>
              <SelectTrigger className="w-[72px]">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {MINUTES.map((m) => (
                  <SelectItem key={m} value={m}>
                    {m}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}
