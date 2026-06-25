import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";

export default function Home() {
  const t = useTranslations("app");
  const tc = useTranslations("common");

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center gap-4 p-8">
      <h1 className="text-3xl font-semibold tracking-tight">{t("name")}</h1>
      <p className="text-muted-foreground">{t("tagline")}</p>
      <div>
        <Button asChild>
          <Link href="/login">{tc("signIn")}</Link>
        </Button>
      </div>
    </main>
  );
}
