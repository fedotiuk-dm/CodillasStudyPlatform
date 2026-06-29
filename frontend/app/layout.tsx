import { GeistMono } from "geist/font/mono";
import { GeistSans } from "geist/font/sans";
import { getLocale } from "next-intl/server";
import type { ReactNode } from "react";

import "@/styles/globals.css";
import { RootProviders } from "./RootProviders";

// Root layout sits ABOVE the [locale] segment, so it never remounts when the language changes.
// That keeps the keycloak-js singleton and the React Query client alive across a locale switch —
// otherwise switching locale would remount the providers and trigger a second keycloak.init()
// (a flash that looked like a full page reload). Locale switching now only re-renders content.
export default async function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  const locale = await getLocale();

  return (
    <html
      lang={locale}
      suppressHydrationWarning
      className={`${GeistSans.variable} ${GeistMono.variable}`}
    >
      <body className="min-h-screen antialiased">
        <RootProviders>{children}</RootProviders>
      </body>
    </html>
  );
}
