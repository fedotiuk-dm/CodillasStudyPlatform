import { Loader2 } from "lucide-react";

import { AppBrand } from "@/components/brand/app-brand";

/** Branded full-screen interstitial shown while Keycloak initializes or redirects to its login. */
export function AuthLoading({ message }: { message?: string }) {
  return (
    <main className="relative grid min-h-screen place-items-center overflow-hidden px-6">
      <div className="absolute -top-40 -right-40 size-[32rem] rounded-full bg-primary/10 blur-3xl" />
      <div className="absolute -bottom-52 -left-40 size-[32rem] rounded-full bg-info/8 blur-3xl" />
      <div className="relative flex w-full max-w-sm flex-col items-center gap-6 rounded-3xl border bg-card/80 px-8 py-12 text-center shadow-xl shadow-primary/5 backdrop-blur">
        <AppBrand />
        <div className="flex items-center gap-2.5 text-muted-foreground">
          <Loader2 className="size-5 animate-spin text-primary" />
          {message ? <p className="text-sm">{message}</p> : null}
        </div>
      </div>
    </main>
  );
}
