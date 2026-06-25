"use client";

import { useTranslations } from "next-intl";

import { RoleGate } from "@/components/auth/role-gate";
import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";
import { useKeycloak } from "@/lib/auth";
import { useNavItems } from "@/lib/navigation";

export default function DashboardPage() {
  return (
    <RoleGate>
      <DashboardContent />
    </RoleGate>
  );
}

function DashboardContent() {
  const t = useTranslations("nav");
  const { name, roles, logout } = useKeycloak();
  const items = useNavItems();

  return (
    <div className="mx-auto max-w-5xl p-8">
      <header className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="text-sm text-muted-foreground">
            {name} · {roles.join(", ") || "—"}
          </p>
        </div>
        <Button variant="outline" onClick={() => void logout()}>
          Sign out
        </Button>
      </header>

      <nav className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => (
          <Link
            key={item.key}
            href={item.href}
            className="flex items-center gap-3 rounded-lg border p-4 transition-colors hover:bg-accent"
          >
            <item.icon className="size-5 text-muted-foreground" />
            <span className="font-medium">{t(item.key)}</span>
          </Link>
        ))}
      </nav>
    </div>
  );
}
