"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import type { ReactNode } from "react";

import { ProfileInit } from "@/components/auth/profile-init";
import { Toaster } from "@/components/ui/sonner";
import { KeycloakProvider } from "@/lib/auth";
import { createQueryClient } from "@/lib/query/create-query-client";
import { ThemeProvider } from "@/providers/theme-provider";

// One QueryClient and one keycloak-js singleton for the whole app — both must survive navigation
// between route groups (a second keycloak.init() would drop the session).
const queryClient = createQueryClient();

export function RootProviders({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <NuqsAdapter>
        <QueryClientProvider client={queryClient}>
          <KeycloakProvider>
            <ProfileInit />
            {children}
            <Toaster richColors position="top-right" />
            {process.env.NODE_ENV === "development" && <ReactQueryDevtools initialIsOpen={false} />}
          </KeycloakProvider>
        </QueryClientProvider>
      </NuqsAdapter>
    </ThemeProvider>
  );
}
