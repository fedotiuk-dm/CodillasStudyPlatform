import { useTranslations } from "next-intl";

import { Badge } from "@/components/ui/badge";
import type { CourseGradeResponse } from "@/lib/api/gradebook/model";

/** The weighted final course grade (Σawarded/Σmax) + per-type (homework/test) breakdown. Dumb. */
export function CourseGradeCard({ grade }: { grade: CourseGradeResponse }) {
  const t = useTranslations("gradebook");

  return (
    <div className="rounded-lg border p-4">
      <div className="flex items-baseline justify-between">
        <span className="text-muted-foreground text-sm">{t("finalGrade")}</span>
        <span className="font-semibold text-2xl">{Math.round(grade.percent)}%</span>
      </div>
      <p className="text-muted-foreground text-xs">
        {grade.awarded}/{grade.maxPoints} {t("points")}
      </p>
      {grade.byType.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {grade.byType.map((b) => (
            <Badge key={b.source} variant="secondary" className="font-normal">
              {t(`sourceTypes.${b.source}`)}: {Math.round(b.percent)}% ({b.awarded}/{b.maxPoints})
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
