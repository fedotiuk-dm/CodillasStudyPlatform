"use client";

import { useTranslations } from "next-intl";
import { useEffect, useRef } from "react";

import { AuthLoading } from "@/components/auth/auth-loading";
import { useRouter } from "@/i18n/navigation";
import { useKeycloak } from "@/lib/auth";

/** Login entry — kicks off Keycloak sign-in, then lands on the dashboard. */
export default function LoginPage() {
  const t = useTranslations("common");
  const router = useRouter();
  const { isInitialized, isAuthenticated, login } = useKeycloak();
  const triggered = useRef(false);

  useEffect(() => {
    if (!isInitialized) return;
    if (isAuthenticated) {
      router.replace("/dashboard");
      return;
    }
    if (triggered.current) return;
    triggered.current = true;
    void login();
  }, [isInitialized, isAuthenticated, login, router]);

  return <AuthLoading message={t("signingIn")} />;
}
