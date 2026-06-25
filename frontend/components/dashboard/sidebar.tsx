"use client";

import { GraduationCap } from "lucide-react";
import { useTranslations } from "next-intl";

import { Link, usePathname } from "@/i18n/navigation";
import { SITE_NAME } from "@/lib/constants";
import { useNavItems } from "@/lib/navigation";
import { cn } from "@/lib/utils";

export function Sidebar() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const items = useNavItems();

  return (
    <aside className="bg-card hidden w-60 shrink-0 flex-col border-r md:flex">
      <Link href="/dashboard" className="flex h-14 items-center gap-2 border-b px-5 font-semibold">
        <GraduationCap className="size-5" />
        <span className="truncate text-sm">{SITE_NAME}</span>
      </Link>
      <nav className="flex flex-1 flex-col gap-0.5 p-3">
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.key}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                active
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
              )}
            >
              <item.icon className="size-4" />
              {t(item.key)}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
