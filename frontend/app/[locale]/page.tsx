import { useTranslations } from "next-intl";

export default function Home() {
  const t = useTranslations("app");

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center gap-3 p-8">
      <h1 className="text-3xl font-semibold tracking-tight">{t("name")}</h1>
      <p className="text-muted-foreground">{t("tagline")}</p>
    </main>
  );
}
