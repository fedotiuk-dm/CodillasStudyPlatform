"use client";

import { useTranslations } from "next-intl";

import { LanguageSwitcher } from "@/components/dashboard/language-switcher";
import { NotificationBell } from "@/components/dashboard/notification-bell";
import { MobileSidebar } from "@/components/dashboard/sidebar";
import { ThemeToggle } from "@/components/dashboard/theme-toggle";
import { UserMenu } from "@/components/dashboard/user-menu";
import { usePathname } from "@/i18n/navigation";
import { useNavItems } from "@/lib/navigation";

export function DashboardTopbar() {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const items = useNavItems();
  const current = items.find(
    (item) =>
      pathname === item.href ||
      (item.href !== "/dashboard" && pathname.startsWith(`${item.href}/`)),
  );

  return (
    <header className="sticky top-0 z-30 border-b bg-background/82 backdrop-blur-xl supports-[backdrop-filter]:bg-background/72">
      <div className="flex h-16 items-center gap-2 px-4 sm:px-6">
        <MobileSidebar />
        <div className="min-w-0 flex-1">
          <p className="truncate font-medium text-sm sm:text-base">
            {current ? t(current.key) : t("dashboard")}
          </p>
        </div>
        <div className="flex items-center gap-0.5 rounded-xl border bg-card/75 p-1 shadow-xs">
          <LanguageSwitcher />
          <ThemeToggle />
          <NotificationBell />
        </div>
        <UserMenu />
      </div>
    </header>
  );
}
