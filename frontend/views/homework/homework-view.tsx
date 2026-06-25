"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";

import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useListGroups } from "@/lib/api/enrollment/enrollment/enrollment";
import {
  getListAssignmentsQueryKey,
  useListAssignments,
  usePublishAssignment,
} from "@/lib/api/homework/homework/homework";
import { useHasAnyRole } from "@/lib/auth";
import { Role } from "@/lib/constants";
import { CreateAssignmentDialog } from "./create-assignment-dialog";
import { SubmissionsDialog } from "./submissions-dialog";

export function HomeworkView() {
  const { data: groupsData } = useListGroups();
  const groups = groupsData?.content ?? [];
  const [groupId, setGroupId] = useState("");
  const queryClient = useQueryClient();
  const canManage = useHasAnyRole([Role.ADMIN, Role.TEACHER]);
  const [open, setOpen] = useState(false);
  const [subsFor, setSubsFor] = useState<{ id: string; title: string } | null>(null);

  const { data, isLoading, isError } = useListAssignments(
    { groupId },
    { query: { enabled: !!groupId } },
  );
  const publish = usePublishAssignment();
  const assignments = data?.content ?? [];

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: getListAssignmentsQueryKey({ groupId }) });

  function onPublish(assignmentId: string) {
    publish.mutate(
      { assignmentId },
      {
        onSuccess: () => {
          toast.success("Assignment published");
          invalidate();
        },
        onError: () => toast.error("Could not publish"),
      },
    );
  }

  return (
    <>
      <PageHeader
        title="Homework"
        description="Assignments and submissions, per group."
        action={
          canManage && groupId ? (
            <Button onClick={() => setOpen(true)}>New assignment</Button>
          ) : undefined
        }
      />

      <div className="mb-4 max-w-xs">
        <Select value={groupId} onValueChange={setGroupId}>
          <SelectTrigger>
            <SelectValue placeholder="Select a group" />
          </SelectTrigger>
          <SelectContent>
            {groups.map((g) => (
              <SelectItem key={g.id} value={g.id}>
                {g.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {groupId && (
        <Card>
          <CardContent className="pt-6">
            <DataState isLoading={isLoading} isError={isError} isEmpty={assignments.length === 0}>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Title</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Due</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assignments.map((a) => (
                    <TableRow key={a.id}>
                      <TableCell className="font-medium">{a.title}</TableCell>
                      <TableCell>
                        <Badge variant={a.status === "PUBLISHED" ? "default" : "secondary"}>
                          {a.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {a.dueAt ? new Date(a.dueAt).toLocaleString() : "—"}
                      </TableCell>
                      <TableCell className="space-x-2 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setSubsFor({ id: a.id, title: a.title })}
                        >
                          Submissions
                        </Button>
                        {canManage && a.status === "DRAFT" && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={publish.isPending}
                            onClick={() => onPublish(a.id)}
                          >
                            Publish
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </DataState>
          </CardContent>
        </Card>
      )}

      {groupId && (
        <CreateAssignmentDialog
          groupId={groupId}
          open={open}
          onOpenChange={setOpen}
          onCreated={invalidate}
        />
      )}

      {subsFor && (
        <SubmissionsDialog
          assignmentId={subsFor.id}
          assignmentTitle={subsFor.title}
          canManage={canManage}
          open={!!subsFor}
          onOpenChange={(o) => !o && setSubsFor(null)}
        />
      )}
    </>
  );
}
