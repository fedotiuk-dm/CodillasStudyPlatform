import { Loader2 } from "lucide-react";

export function AuthLoading({ message }: { message?: string }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 text-muted-foreground">
      <Loader2 className="size-6 animate-spin" />
      {message ? <p className="text-sm">{message}</p> : null}
    </div>
  );
}
