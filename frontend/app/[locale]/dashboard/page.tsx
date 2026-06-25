"use client";

import { useTranslations } from "next-intl";

import { PageHeader } from "@/components/shared/page-header";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Link } from "@/i18n/navigation";
import { useKeycloak } from "@/lib/auth";
import { useNavItems } from "@/lib/navigation";

export default function DashboardPage() {
  const t = useTranslations("nav");
  const { name } = useKeycloak();
  const items = useNavItems();

  return (
    <>
      <PageHeader title="Dashboard" description={name ? `Welcome back, ${name}.` : undefined} />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((item) => (
          <Link key={item.key} href={item.href}>
            <Card className="hover:border-ring transition-colors">
              <CardHeader>
                <item.icon className="text-muted-foreground size-6" />
                <CardTitle className="mt-2">{t(item.key)}</CardTitle>
                <CardDescription>Open {t(item.key).toLowerCase()}</CardDescription>
              </CardHeader>
            </Card>
          </Link>
        ))}
      </div>
    </>
  );
}
