import { ArrowRight, BookOpenCheck, MessagesSquare, TrendingUp } from "lucide-react";
import { useTranslations } from "next-intl";

import { AppBrand } from "@/components/brand/app-brand";
import { LanguageSwitcher } from "@/components/dashboard/language-switcher";
import { ThemeToggle } from "@/components/dashboard/theme-toggle";
import { Button } from "@/components/ui/button";
import { Link } from "@/i18n/navigation";
import { SITE_NAME } from "@/lib/constants";

export default function Home() {
  const t = useTranslations("app");
  const tc = useTranslations("common");

  return (
    <main className="relative min-h-screen overflow-hidden">
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-primary/60 to-transparent" />
      <div className="absolute -top-40 -right-40 size-[32rem] rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute -bottom-52 -left-40 size-[32rem] rounded-full bg-info/8 blur-3xl" />

      <div className="relative mx-auto flex min-h-screen max-w-7xl flex-col px-5 sm:px-8">
        <header className="flex h-20 items-center justify-between">
          <AppBrand />
          <div className="flex items-center gap-1.5">
            <LanguageSwitcher />
            <ThemeToggle />
            <Button asChild variant="outline" className="ml-1 rounded-xl bg-card/70">
              <Link href="/login">{tc("signIn")}</Link>
            </Button>
          </div>
        </header>

        <section className="grid flex-1 items-center gap-12 py-16 lg:grid-cols-[1.05fr_0.95fr] lg:py-24">
          <div className="max-w-2xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/8 px-3 py-1.5 font-medium text-primary text-xs">
              <span className="size-1.5 rounded-full bg-primary" />
              {t("eyebrow")}
            </div>
            <h1 className="mt-6 text-balance font-semibold text-4xl tracking-[-0.035em] sm:text-6xl">
              {t("heroTitle")}
            </h1>
            <p className="mt-5 max-w-xl text-pretty text-lg text-muted-foreground leading-relaxed">
              {t("tagline")}. {t("heroDescription")}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button asChild size="lg" className="rounded-xl">
                <Link href="/login">
                  {tc("signIn")}
                  <ArrowRight className="size-4" />
                </Link>
              </Button>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-xl">
            <div className="absolute inset-6 rounded-3xl bg-primary/15 blur-3xl" />
            <div className="relative overflow-hidden rounded-3xl border bg-card/88 p-4 shadow-2xl shadow-primary/10 backdrop-blur sm:p-6">
              <div className="mb-6 flex items-center gap-2">
                <span className="size-2.5 rounded-full bg-destructive/70" />
                <span className="size-2.5 rounded-full bg-warning/70" />
                <span className="size-2.5 rounded-full bg-success/70" />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                {[
                  {
                    icon: BookOpenCheck,
                    title: t("featureLearning"),
                    tone: "text-primary bg-primary/10",
                  },
                  {
                    icon: TrendingUp,
                    title: t("featureProgress"),
                    tone: "text-success bg-success/10",
                  },
                  {
                    icon: MessagesSquare,
                    title: t("featureCommunication"),
                    tone: "text-info bg-info/10",
                  },
                ].map((feature, index) => (
                  <div
                    key={feature.title}
                    className={
                      index === 0
                        ? "rounded-2xl border p-5 sm:col-span-2"
                        : "rounded-2xl border p-5"
                    }
                  >
                    <span className={`grid size-10 place-items-center rounded-xl ${feature.tone}`}>
                      <feature.icon className="size-5" />
                    </span>
                    <p className="mt-5 font-semibold">{feature.title}</p>
                    <div className="mt-3 grid gap-2">
                      <span className="h-2 rounded-full bg-muted" />
                      <span className="h-2 w-2/3 rounded-full bg-muted" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        <footer className="flex flex-col items-center justify-between gap-2 border-t py-6 text-muted-foreground text-sm sm:flex-row">
          <span>
            © {new Date().getFullYear()} {SITE_NAME}
          </span>
          <span>{t("tagline")}</span>
        </footer>
      </div>
    </main>
  );
}
