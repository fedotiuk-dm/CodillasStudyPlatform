import { defineRouting } from "next-intl/routing";

export const routing = defineRouting({
  locales: ["uk", "en", "de"],
  defaultLocale: "uk",
  // Every page sits under /{locale}/... so each language has its own URL (real hreflang SEO).
  localePrefix: "always",
});

export type AppLocale = (typeof routing.locales)[number];
