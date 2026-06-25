import { MutationCache, QueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { extractErrorMessage } from "@/lib/api-error";

/** One QueryClient for the whole app so the cache survives navigation between route groups. */
export function createQueryClient(): QueryClient {
  let client: QueryClient;
  // ponytail: any successful mutation invalidates every query. No per-endpoint rules table to
  // maintain — over-fetches a little, invisible at admin-LMS query volume. Add a mutationKey→prefix
  // rules map (see Boosting's invalidation-rules.ts) only if refetch traffic ever becomes a problem.
  const mutationCache = new MutationCache({
    onSuccess: () => client.invalidateQueries(),
    // Global error toast for every mutation. Opt out per-mutation with meta.skipGlobalErrorToast.
    onError: (error, _vars, _ctx, mutation) => {
      if (mutation.meta?.skipGlobalErrorToast) return;
      toast.error(extractErrorMessage(error));
    },
  });
  client = new QueryClient({
    mutationCache,
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: true,
        refetchOnMount: true,
        retry: 1,
        staleTime: 30_000,
      },
    },
  });
  return client;
}
