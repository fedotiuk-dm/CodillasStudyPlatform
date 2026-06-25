import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const isDev = process.env.NODE_ENV === "development";

const nextConfig: NextConfig = {
  output: isDev ? undefined : "standalone",
  reactStrictMode: true,
  poweredByHeader: false,
  typedRoutes: true,
  // React Compiler: auto-memoizes components/hooks (no manual useMemo/useCallback).
  reactCompiler: true,
};

export default withNextIntl(nextConfig);
