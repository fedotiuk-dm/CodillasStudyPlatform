import type { ReactNode } from "react";

import { Sidebar } from "@/components/dashboard/sidebar";
import { ThemeToggle } from "@/components/dashboard/theme-toggle";
import { UserMenu } from "@/components/dashboard/user-menu";

export function DashboardShell({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="bg-card/80 sticky top-0 z-30 flex h-14 items-center justify-end gap-1 border-b px-4 backdrop-blur">
          <ThemeToggle />
          <UserMenu />
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
