import { useTranslations } from "next-intl";

import type { NotificationResponse } from "@/lib/api/notification/model";

/** Substitute `{key}` tokens from `params`; if any token has no value, return the fallback instead. */
function fillTemplate(
  template: string,
  params: Record<string, string> | undefined,
  fallback: string | undefined,
): string | undefined {
  const tokens = [...template.matchAll(/\{(\w+)\}/g)].map((m) => m[1]);
  if (tokens.some((k) => params?.[k] == null)) return fallback;
  return template.replace(/\{(\w+)\}/g, (_, k) => params?.[k] ?? "");
}

/**
 * Renders an in-app notification's title/body in the VIEWER's UI language from its `type` + `params`
 * (so it follows the language switcher, unlike the server-rendered text which is frozen at the
 * default locale). Titles have no params. Bodies are filled manually from the raw template — never
 * via next-intl's strict ICU formatting — so a missing `{points}` (legacy rows, empty params) falls
 * back to the server-rendered text instead of throwing a formatting error.
 */
export function useLocalizedNotification() {
  const t = useTranslations("notifications");
  return (n: NotificationResponse) => {
    const titleKey = `types.${n.type}.title`;
    const bodyKey = `types.${n.type}.body`;
    return {
      title: t.has(titleKey) ? t(titleKey) : n.title,
      body: t.has(bodyKey) ? fillTemplate(String(t.raw(bodyKey)), n.params, n.body) : n.body,
    };
  };
}
