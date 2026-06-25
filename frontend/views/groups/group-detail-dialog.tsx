"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  getListGroupMembersQueryKey,
  getListScheduledLessonsQueryKey,
  useEnrollStudent,
  useListGroupMembers,
  useListScheduledLessons,
  useMarkAttendance,
  useScheduleLesson,
} from "@/lib/api/enrollment/enrollment/enrollment";
import type { ScheduledLessonResponse } from "@/lib/api/enrollment/model";

export function GroupDetailDialog({
  groupId,
  groupName,
  open,
  onOpenChange,
}: {
  groupId: string;
  groupName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{groupName}</DialogTitle>
          <DialogDescription>Manage members and scheduled lessons.</DialogDescription>
        </DialogHeader>
        <Tabs defaultValue="members">
          <TabsList>
            <TabsTrigger value="members">Members</TabsTrigger>
            <TabsTrigger value="lessons">Lessons</TabsTrigger>
          </TabsList>
          <TabsContent value="members">
            <MembersTab groupId={groupId} open={open} />
          </TabsContent>
          <TabsContent value="lessons">
            <LessonsTab groupId={groupId} open={open} />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}

function MembersTab({ groupId, open }: { groupId: string; open: boolean }) {
  const queryClient = useQueryClient();
  const { data } = useListGroupMembers(groupId, { query: { enabled: open } });
  const members = data ?? [];
  const enroll = useEnrollStudent();
  const [userId, setUserId] = useState("");

  function onEnroll() {
    if (!userId.trim()) return;
    enroll.mutate(
      { groupId, data: { userId: userId.trim() } },
      {
        onSuccess: () => {
          toast.success("Student enrolled");
          setUserId("");
          queryClient.invalidateQueries({ queryKey: getListGroupMembersQueryKey(groupId) });
        },
        onError: () => toast.error("Could not enroll"),
      },
    );
  }

  return (
    <div className="grid gap-3 pt-3">
      {members.length === 0 && <p className="text-muted-foreground text-sm">No members yet.</p>}
      <ul className="grid gap-1 text-sm">
        {members.map((m) => (
          <li key={m.id} className="rounded border px-3 py-2 font-mono text-xs">
            {m.userId}
          </li>
        ))}
      </ul>
      <Separator />
      <div className="flex items-center gap-2">
        <Input
          placeholder="Keycloak user id (UUID)"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
        <Button disabled={enroll.isPending || !userId.trim()} onClick={onEnroll}>
          Enroll
        </Button>
      </div>
    </div>
  );
}

function LessonsTab({ groupId, open }: { groupId: string; open: boolean }) {
  const queryClient = useQueryClient();
  const { data } = useListScheduledLessons(groupId, { query: { enabled: open } });
  const lessons = data ?? [];
  const schedule = useScheduleLesson();
  const [title, setTitle] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [meetLink, setMeetLink] = useState("");

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: getListScheduledLessonsQueryKey(groupId) });

  function onSchedule() {
    if (!title.trim() || !scheduledAt) return;
    schedule.mutate(
      {
        groupId,
        data: {
          title,
          scheduledAt: new Date(scheduledAt).toISOString(),
          meetLink: meetLink || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success("Lesson scheduled");
          setTitle("");
          setScheduledAt("");
          setMeetLink("");
          invalidate();
        },
        onError: () => toast.error("Could not schedule"),
      },
    );
  }

  return (
    <div className="grid gap-3 pt-3">
      {lessons.length === 0 && <p className="text-muted-foreground text-sm">No lessons yet.</p>}
      {lessons.map((l) => (
        <LessonRow key={l.id} groupId={groupId} lesson={l} />
      ))}
      <Separator />
      <div className="grid gap-2">
        <p className="font-medium text-sm">Schedule a lesson</p>
        <Input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <Input
          type="datetime-local"
          value={scheduledAt}
          onChange={(e) => setScheduledAt(e.target.value)}
        />
        <Input
          placeholder="Meet link (optional)"
          value={meetLink}
          onChange={(e) => setMeetLink(e.target.value)}
        />
        <Button
          className="justify-self-start"
          disabled={schedule.isPending || !title.trim() || !scheduledAt}
          onClick={onSchedule}
        >
          Schedule
        </Button>
      </div>
    </div>
  );
}

function LessonRow({ groupId, lesson }: { groupId: string; lesson: ScheduledLessonResponse }) {
  const mark = useMarkAttendance();
  const [userId, setUserId] = useState("");

  function setPresence(present: boolean) {
    if (!userId.trim()) return;
    mark.mutate(
      {
        groupId,
        scheduledLessonId: lesson.id,
        data: { userId: userId.trim(), present },
      },
      {
        onSuccess: () => {
          toast.success(present ? "Marked present" : "Marked absent");
          setUserId("");
        },
        onError: () => toast.error("Could not mark attendance"),
      },
    );
  }

  return (
    <div className="grid gap-2 rounded-md border p-3 text-sm">
      <div>
        <p className="font-medium">{lesson.title}</p>
        <p className="text-muted-foreground">{new Date(lesson.scheduledAt).toLocaleString()}</p>
        {lesson.meetLink && (
          <a
            href={lesson.meetLink}
            className="text-primary underline"
            target="_blank"
            rel="noreferrer"
          >
            Join link
          </a>
        )}
      </div>
      <div className="flex items-center gap-2">
        <Input
          className="flex-1"
          placeholder="Student id for attendance"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
        <Button
          variant="outline"
          size="sm"
          disabled={mark.isPending}
          onClick={() => setPresence(true)}
        >
          Present
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={mark.isPending}
          onClick={() => setPresence(false)}
        >
          Absent
        </Button>
      </div>
    </div>
  );
}
