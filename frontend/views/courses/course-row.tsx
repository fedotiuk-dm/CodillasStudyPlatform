import { useTranslations } from "next-intl";

import { StatusBadge } from "@/components/shared/status-badge";
import { TableCell, TableRow } from "@/components/ui/table";
import { Link } from "@/i18n/navigation";
import type { CourseResponse } from "@/lib/api/course/model";

import { CourseRowActions } from "./course-row-actions";

/** One course row: name link, description, status badge, and (for staff) lifecycle actions. */
export function CourseRow({ course, canManage }: { course: CourseResponse; canManage: boolean }) {
  const t = useTranslations("courses");

  return (
    <TableRow>
      <TableCell className="font-medium">
        <Link href={`/dashboard/courses/${course.id}`} className="hover:underline">
          {course.name}
        </Link>
      </TableCell>
      <TableCell className="text-muted-foreground">{course.description ?? "—"}</TableCell>
      <TableCell>
        <StatusBadge status={course.status} label={t(`status.${course.status}`)} />
      </TableCell>
      <TableCell className="text-right">
        {canManage && <CourseRowActions course={course} />}
      </TableCell>
    </TableRow>
  );
}
