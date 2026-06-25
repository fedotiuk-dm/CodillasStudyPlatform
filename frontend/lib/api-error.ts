import { isAxiosError } from "axios";

/** Pull a human message out of a failed request. Backend errors are RFC-7807 ProblemDetail. */
export function extractErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    const data = error.response?.data as { detail?: string; title?: string } | undefined;
    return data?.detail ?? data?.title ?? error.message;
  }
  return error instanceof Error ? error.message : "Something went wrong";
}
