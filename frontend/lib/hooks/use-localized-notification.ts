import { useTranslations } from "next-intl";

import type { NotificationResponse } from "@/lib/api/notification/model";

/**
 * Renders an in-app notification's title/body in the VIEWER's UI language from its `type` + `params`
 * (so it follows the language switcher, unlike the server-rendered text which is frozen at the
 * default locale). Falls back to the server-stored title/body for any unknown type.
 */
export function useLocalizedNotification() {
  const t = useTranslations("notifications");
  return (n: NotificationResponse) => ({
    title: t.has(`types.${n.type}.title`) ? t(`types.${n.type}.title`) : n.title,
    body: t.has(`types.${n.type}.body`) ? t(`types.${n.type}.body`, n.params) : n.body,
  });
}
