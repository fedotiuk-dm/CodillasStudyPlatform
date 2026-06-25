import type { ReactNode } from "react";

import { RoleGate } from "@/components/auth/role-gate";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";

export default function DashboardLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <RoleGate>
      <DashboardShell>{children}</DashboardShell>
    </RoleGate>
  );
}
