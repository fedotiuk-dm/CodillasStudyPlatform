import type { ReactNode } from "react";

import { DashboardTopbar } from "@/components/dashboard/dashboard-topbar";
import { Sidebar } from "@/components/dashboard/sidebar";

export function DashboardShell({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <DashboardTopbar />
        <main className="flex-1 px-4 py-5 sm:px-6 sm:py-6">
          <div className="mx-auto w-full max-w-[96rem]">{children}</div>
        </main>
      </div>
    </div>
  );
}
