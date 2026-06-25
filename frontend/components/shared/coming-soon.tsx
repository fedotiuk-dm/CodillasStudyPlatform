import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent } from "@/components/ui/card";

/** Placeholder for nav modules not wired to their API yet (built module-by-module). */
export function ComingSoon({ title }: { title: string }) {
  return (
    <>
      <PageHeader title={title} />
      <Card>
        <CardContent className="text-muted-foreground py-12 text-center text-sm">
          This module is coming soon.
        </CardContent>
      </Card>
    </>
  );
}
