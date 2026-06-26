"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { toast } from "sonner";

import { UserPicker } from "@/components/shared/user-picker";
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
  useEnrollStudent,
  useListAttendance,
  useListGroupMembers,
  useListScheduledLessons,
  useMarkAttendance,
  useScheduleLesson,
} from "@/lib/api/enrollment/enrollment/enrollment";
import type { ScheduledLessonResponse } from "@/lib/api/enrollment/model";
import { useProfileNames } from "@/lib/hooks/use-profile-names";

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
  const t = useTranslations("groups");
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{groupName}</DialogTitle>
          <DialogDescription>{t("manageDescription")}</DialogDescription>
        </DialogHeader>
        <Tabs defaultValue="members">
          <TabsList>
            <TabsTrigger value="members">{t("members")}</TabsTrigger>
            <TabsTrigger value="lessons">{t("lessons")}</TabsTrigger>
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
  const t = useTranslations("groups");
  const { data } = useListGroupMembers(groupId, { query: { enabled: open } });
  const members = data ?? [];
  const enroll = useEnrollStudent();
  const nameOf = useProfileNames();
  const [picked, setPicked] = useState<{ userId: string; displayName: string }>();

  function onEnroll() {
    if (!picked) return;
    enroll.mutate(
      { groupId, data: { userId: picked.userId } },
      {
        onSuccess: () => {
          toast.success(t("studentEnrolled"));
          setPicked(undefined);
        },
      },
    );
  }

  return (
    <div className="grid gap-3 pt-3">
      {members.length === 0 && <p className="text-muted-foreground text-sm">{t("noMembers")}</p>}
      <ul className="grid gap-1 text-sm">
        {members.map((m) => (
          <li key={m.id} className="rounded border px-3 py-2">
            {nameOf(m.userId)}
          </li>
        ))}
      </ul>
      <Separator />
      <div className="flex items-start gap-2">
        <div className="flex-1">
          <UserPicker
            value={picked?.userId}
            displayName={picked?.displayName}
            placeholder={t("searchStudents")}
            onChange={(userId, displayName) => setPicked({ userId, displayName })}
          />
        </div>
        <Button disabled={enroll.isPending || !picked} onClick={onEnroll}>
          {t("enroll")}
        </Button>
      </div>
    </div>
  );
}

function LessonsTab({ groupId, open }: { groupId: string; open: boolean }) {
  const t = useTranslations("groups");
  const { data } = useListScheduledLessons(groupId, { query: { enabled: open } });
  const lessons = data ?? [];
  const schedule = useScheduleLesson();
  const [title, setTitle] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [meetLink, setMeetLink] = useState("");

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
          toast.success(t("lessonScheduled"));
          setTitle("");
          setScheduledAt("");
          setMeetLink("");
        },
      },
    );
  }

  return (
    <div className="grid gap-3 pt-3">
      {lessons.length === 0 && <p className="text-muted-foreground text-sm">{t("noLessons")}</p>}
      {lessons.map((l) => (
        <LessonRow key={l.id} groupId={groupId} lesson={l} />
      ))}
      <Separator />
      <div className="grid gap-2">
        <p className="font-medium text-sm">{t("scheduleLesson")}</p>
        <Input
          placeholder={t("lessonTitlePlaceholder")}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <Input
          type="datetime-local"
          value={scheduledAt}
          onChange={(e) => setScheduledAt(e.target.value)}
        />
        <Input
          placeholder={t("meetLinkPlaceholder")}
          value={meetLink}
          onChange={(e) => setMeetLink(e.target.value)}
        />
        <Button
          className="justify-self-start"
          disabled={schedule.isPending || !title.trim() || !scheduledAt}
          onClick={onSchedule}
        >
          {t("schedule")}
        </Button>
      </div>
    </div>
  );
}

function LessonRow({ groupId, lesson }: { groupId: string; lesson: ScheduledLessonResponse }) {
  const t = useTranslations("groups");
  const mark = useMarkAttendance();
  const nameOf = useProfileNames();
  // Marking invalidates queries globally (central MutationCache), so this refetches after a mark.
  const { data: attendance } = useListAttendance(groupId, lesson.id);
  const records = attendance ?? [];
  const [picked, setPicked] = useState<{ userId: string; displayName: string }>();

  function setPresence(present: boolean) {
    if (!picked) return;
    mark.mutate(
      {
        groupId,
        scheduledLessonId: lesson.id,
        data: { userId: picked.userId, present },
      },
      {
        onSuccess: () => {
          toast.success(present ? t("markedPresent") : t("markedAbsent"));
          setPicked(undefined);
        },
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
            {t("joinLink")}
          </a>
        )}
      </div>
      {records.length > 0 && (
        <ul className="grid gap-1">
          {records.map((r) => (
            <li key={r.id} className="flex items-center justify-between rounded border px-2 py-1">
              <span>{nameOf(r.userId)}</span>
              <span className={r.present ? "text-green-600" : "text-muted-foreground"}>
                {r.present ? t("present") : t("absent")}
              </span>
            </li>
          ))}
        </ul>
      )}
      <div className="flex items-center gap-2">
        <div className="flex-1">
          <UserPicker
            value={picked?.userId}
            displayName={picked?.displayName}
            placeholder={t("searchStudent")}
            onChange={(userId, displayName) => setPicked({ userId, displayName })}
          />
        </div>
        <Button
          variant="outline"
          size="sm"
          disabled={mark.isPending}
          onClick={() => setPresence(true)}
        >
          {t("present")}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          disabled={mark.isPending}
          onClick={() => setPresence(false)}
        >
          {t("absent")}
        </Button>
      </div>
    </div>
  );
}
