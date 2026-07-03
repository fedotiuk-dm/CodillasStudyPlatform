/** Minimal iCalendar (RFC 5545) builder for the personal schedule export. */

export interface IcsEvent {
  id: string;
  title: string;
  /** ISO timestamp. */
  start: string;
  /** Minutes; lessons carry no duration, callers pass a sensible default. */
  durationMinutes: number;
  description?: string;
  url?: string;
}

function icsEscape(text: string): string {
  return text
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\r?\n/g, "\\n");
}

/** UTC basic format: 20260901T100000Z */
function icsDate(date: Date): string {
  return date
    .toISOString()
    .replace(/[-:]/g, "")
    .replace(/\.\d{3}/, "");
}

export function buildIcs(events: IcsEvent[]): string {
  const now = icsDate(new Date());
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Codillas//Study Platform//EN",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
  ];
  for (const event of events) {
    const start = new Date(event.start);
    const end = new Date(start.getTime() + event.durationMinutes * 60_000);
    lines.push(
      "BEGIN:VEVENT",
      `UID:${event.id}@codillas`,
      `DTSTAMP:${now}`,
      `DTSTART:${icsDate(start)}`,
      `DTEND:${icsDate(end)}`,
      `SUMMARY:${icsEscape(event.title)}`,
    );
    if (event.description) lines.push(`DESCRIPTION:${icsEscape(event.description)}`);
    if (event.url) lines.push(`URL:${icsEscape(event.url)}`);
    lines.push("END:VEVENT");
  }
  lines.push("END:VCALENDAR");
  // RFC 5545 requires CRLF line endings.
  return `${lines.join("\r\n")}\r\n`;
}

export function downloadIcs(filename: string, events: IcsEvent[]): void {
  const blob = new Blob([buildIcs(events)], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
