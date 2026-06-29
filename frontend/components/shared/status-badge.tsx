import { Badge } from "@/components/ui/badge";

/** Tone per lifecycle status, shared by course (DRAFT/PUBLISHED/ARCHIVED) and group (DRAFT/RUNNING/ARCHIVED). */
const TONE: Record<string, "default" | "secondary" | "outline"> = {
  DRAFT: "secondary",
  PUBLISHED: "default",
  RUNNING: "default",
  ARCHIVED: "outline",
};

/** A lifecycle-status pill. Dumb: the caller resolves the i18n `label`; this only picks the tone. */
export function StatusBadge({ status, label }: { status: string; label: string }) {
  return <Badge variant={TONE[status] ?? "secondary"}>{label}</Badge>;
}
