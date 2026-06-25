import { QueryClient } from "@tanstack/react-query";

/** One QueryClient for the whole app so the cache survives navigation between route groups. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: true,
        refetchOnMount: true,
        retry: 1,
        staleTime: 30_000,
      },
    },
  });
}
