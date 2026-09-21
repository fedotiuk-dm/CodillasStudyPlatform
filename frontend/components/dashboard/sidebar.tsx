"use client";

import { Menu } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { AppBrand } from "@/components/brand/app-brand";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { Link, usePathname } from "@/i18n/navigation";
import { useKeycloak, usePrimaryRole } from "@/lib/auth";
import { useNavGroups } from "@/lib/navigation";
import { cn } from "@/lib/utils";

function isActivePath(pathname: string, href: string) {
  return pathname === href || (href !== "/dashboard" && pathname.startsWith(`${href}/`));
}

function NavigationContent({ onNavigate }: { onNavigate?: () => void }) {
  const t = useTranslations("nav");
  const groups = useNavGroups();
  const pathname = usePathname();

  return (
    <nav className="flex-1 overflow-y-auto px-3 py-4">
      {groups.map((group) => (
        <div key={group.key} className="mb-5 last:mb-0">
          <p className="mb-1.5 px-3 font-semibold text-[0.68rem] text-sidebar-muted uppercase tracking-[0.14em]">
            {t(`sections.${group.key}`)}
          </p>
          <div className="grid gap-1">
            {group.items.map((item) => {
              const active = isActivePath(pathname, item.href);
              return (
                <Link
                  key={item.key}
                  href={item.href}
                  onClick={onNavigate}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "group relative flex min-h-10 items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-all",
                    active
                      ? "bg-sidebar-accent text-primary shadow-[inset_0_0_0_1px_color-mix(in_oklch,var(--primary)_12%,transparent)]"
                      : "text-sidebar-muted hover:bg-sidebar-accent/60 hover:text-sidebar-foreground",
                  )}
                >
                  <span
                    className={cn(
                      "grid size-7 shrink-0 place-items-center rounded-lg transition-colors",
                      active ? "bg-primary/12 text-primary" : "group-hover:bg-background/70",
                    )}
                  >
                    <item.icon className="size-4" />
                  </span>
                  <span className="truncate">{t(item.key)}</span>
                  {active && <span className="ml-auto size-1.5 rounded-full bg-primary" />}
                </Link>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );
}

function SidebarFooter() {
  const t = useTranslations("nav");
  const { name } = useKeycloak();
  const role = usePrimaryRole();

  return (
    <div className="m-3 rounded-xl border border-sidebar-border bg-background/55 p-3">
      <p className="truncate font-medium text-sidebar-foreground text-sm">{name ?? "—"}</p>
      <p className="mt-1 text-sidebar-muted text-xs">{t(`roles.${role}`)}</p>
    </div>
  );
}

function SidebarBody({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <>
      <Link
        href="/dashboard"
        onClick={onNavigate}
        className="flex h-18 items-center border-sidebar-border border-b px-5"
      >
        <AppBrand />
      </Link>
      <NavigationContent onNavigate={onNavigate} />
      <SidebarFooter />
    </>
  );
}

export function Sidebar() {
  return (
    <aside className="sticky top-0 hidden h-screen w-68 shrink-0 flex-col border-sidebar-border border-r bg-sidebar text-sidebar-foreground lg:flex">
      <SidebarBody />
    </aside>
  );
}

export function MobileSidebar() {
  const t = useTranslations("nav");
  const [open, setOpen] = useState(false);

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" className="lg:hidden" aria-label={t("openMenu")}>
          <Menu className="size-5" />
        </Button>
      </SheetTrigger>
      <SheetContent
        side="left"
        className="w-[19rem] gap-0 border-sidebar-border bg-sidebar p-0 text-sidebar-foreground sm:max-w-[19rem]"
      >
        <SheetTitle className="sr-only">{t("menu")}</SheetTitle>
        <SheetDescription className="sr-only">{t("menuDescription")}</SheetDescription>
        <SidebarBody onNavigate={() => setOpen(false)} />
      </SheetContent>
    </Sheet>
  );
}
