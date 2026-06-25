import createMiddleware from "next-intl/middleware";

import { routing } from "@/i18n/routing";

// Next.js 16 renamed middleware.ts → proxy.ts for the edge request handler. The i18n proxy gives
// every page a /{locale}/... prefix. API routes, framework paths and files with an extension are
// excluded via the matcher. Role-based authorization is enforced client-side (keycloak-js keeps the
// JWT in browser memory, not a cookie, so this server-side proxy cannot read it).
export default createMiddleware(routing);

export const config = {
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
