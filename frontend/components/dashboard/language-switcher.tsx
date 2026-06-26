"use client";

import { Languages } from "lucide-react";
import { useLocale } from "next-intl";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { usePathname, useRouter } from "@/i18n/navigation";
import { routing } from "@/i18n/routing";

// Languages are shown in their own name (endonyms) — never translated.
const LOCALE_LABELS: Record<string, string> = {
  uk: "Українська",
  en: "English",
  de: "Deutsch",
};

export function LanguageSwitcher() {
  const locale = useLocale();
  const router = useRouter();
  const pathname = usePathname();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Change language">
          <Languages className="size-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {routing.locales.map((l) => (
          <DropdownMenuItem
            key={l}
            disabled={l === locale}
            // Same page, new locale — next-intl keeps the /{locale} prefix in sync.
            onClick={() => router.replace(pathname, { locale: l })}
          >
            {LOCALE_LABELS[l] ?? l}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
