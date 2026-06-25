import { createNavigation } from "next-intl/navigation";

import { routing } from "./routing";

/**
 * Locale-aware navigation primitives. Use Link/redirect/usePathname/useRouter from here so every
 * navigation keeps the /{locale} prefix.
 */
export const { Link, redirect, usePathname, useRouter, getPathname } = createNavigation(routing);
