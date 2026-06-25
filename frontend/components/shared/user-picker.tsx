"use client";

import { Check, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import type { UserProfile } from "@/lib/api/user/model";
import { useListProfiles } from "@/lib/api/user/user/user";

/** Debounce any fast-changing value (no extra dependency). */
export function useDebounced<T>(value: T, ms = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(id);
  }, [value, ms]);
  return debounced;
}

/** Resolve userId → display name for showing names instead of UUIDs in lists. */
// ponytail: loads up to 100 profiles; fine for a school-sized dev instance.
export function useProfileNames() {
  const { data } = useListProfiles({ size: 100 });
  const map = new Map((data?.content ?? []).map((p) => [p.userId, p.displayName]));
  return (userId: string) => map.get(userId) ?? userId;
}

function useProfileSearch(query: string) {
  const q = useDebounced(query.trim());
  const { data, isLoading } = useListProfiles(
    { q: q || undefined, size: 10 },
    { query: { enabled: true } },
  );
  return { results: data?.content ?? [], isLoading };
}

function ResultList({
  results,
  isLoading,
  selectedIds,
  onPick,
}: {
  results: UserProfile[];
  isLoading: boolean;
  selectedIds: string[];
  onPick: (p: UserProfile) => void;
}) {
  return (
    <div className="absolute z-50 mt-1 max-h-56 w-full overflow-y-auto rounded-md border bg-popover p-1 shadow-md">
      {isLoading && <p className="px-2 py-1.5 text-muted-foreground text-sm">Searching…</p>}
      {!isLoading && results.length === 0 && (
        <p className="px-2 py-1.5 text-muted-foreground text-sm">No people found.</p>
      )}
      {results.map((p) => (
        <button
          type="button"
          key={p.userId}
          onMouseDown={(e) => {
            e.preventDefault();
            onPick(p);
          }}
          className="flex w-full items-center justify-between rounded px-2 py-1.5 text-left text-sm hover:bg-accent"
        >
          {p.displayName}
          {selectedIds.includes(p.userId) && <Check className="size-4" />}
        </button>
      ))}
    </div>
  );
}

/** Single-user picker. Emits the selected Keycloak user id (and its display name). */
export function UserPicker({
  value,
  displayName,
  onChange,
  placeholder = "Search people…",
}: {
  value?: string;
  displayName?: string;
  onChange: (userId: string, displayName: string) => void;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const { results, isLoading } = useProfileSearch(query);
  const ref = useRef<HTMLDivElement>(null);

  return (
    <div ref={ref} className="relative">
      <Input
        value={open ? query : (displayName ?? "")}
        placeholder={value && !open ? displayName : placeholder}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onChange={(e) => setQuery(e.target.value)}
      />
      {open && (
        <ResultList
          results={results}
          isLoading={isLoading}
          selectedIds={value ? [value] : []}
          onPick={(p) => {
            onChange(p.userId, p.displayName);
            setQuery("");
            setOpen(false);
          }}
        />
      )}
    </div>
  );
}

/** Multi-user picker. Maintains a set of selected users shown as removable chips. */
export function UserMultiPicker({
  value,
  onChange,
  placeholder = "Add people…",
}: {
  value: { userId: string; displayName: string }[];
  onChange: (users: { userId: string; displayName: string }[]) => void;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const { results, isLoading } = useProfileSearch(query);
  const selectedIds = value.map((u) => u.userId);

  function toggle(p: UserProfile) {
    if (selectedIds.includes(p.userId)) {
      onChange(value.filter((u) => u.userId !== p.userId));
    } else {
      onChange([...value, { userId: p.userId, displayName: p.displayName }]);
    }
  }

  return (
    <div className="grid gap-2">
      {value.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {value.map((u) => (
            <Badge key={u.userId} variant="secondary" className="gap-1">
              {u.displayName}
              <button
                type="button"
                onClick={() => onChange(value.filter((x) => x.userId !== u.userId))}
              >
                <X className="size-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
      <div className="relative">
        <Input
          value={query}
          placeholder={placeholder}
          onFocus={() => setOpen(true)}
          onBlur={() => setOpen(false)}
          onChange={(e) => setQuery(e.target.value)}
        />
        {open && (
          <ResultList
            results={results}
            isLoading={isLoading}
            selectedIds={selectedIds}
            onPick={toggle}
          />
        )}
      </div>
    </div>
  );
}
