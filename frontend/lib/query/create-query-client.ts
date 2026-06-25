import { QueryClient, type QueryClientConfig } from "@tanstack/react-query";

interface QueryDefaults {
  refetchOnWindowFocus?: boolean;
  refetchOnMount?: boolean;
  retry?: number;
  staleTime?: number;
}

/** One QueryClient for the whole app so the cache survives navigation between route groups. */
export function createQueryClient(defaults: QueryDefaults = {}): QueryClient {
  const config: QueryClientConfig = {
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: defaults.refetchOnWindowFocus ?? true,
        refetchOnMount: defaults.refetchOnMount ?? true,
        retry: defaults.retry ?? 1,
        staleTime: defaults.staleTime ?? 30_000,
      },
    },
  };
  return new QueryClient(config);
}
