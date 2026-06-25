"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";

import { FormDialog } from "@/components/shared/form-dialog";
import { UserMultiPicker } from "@/components/shared/user-picker";
import { FormControl, FormField, FormItem, FormLabel } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useCreateRoom } from "@/lib/api/chat/chat/chat";
import { ChatRoomType } from "@/lib/api/chat/model";

type FormValues = { type: ChatRoomType; name: string };

export function CreateRoomDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}) {
  const form = useForm<FormValues>({
    defaultValues: { type: ChatRoomType.GROUP, name: "" },
  });
  const createRoom = useCreateRoom();
  const [members, setMembers] = useState<{ userId: string; displayName: string }[]>([]);

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title="New room"
      description="Start a conversation with one or more members."
      form={form}
      onCreated={onCreated}
      onReset={() => setMembers([])}
      submitDisabled={members.length === 0}
      success="Room created"
      error="Could not create the room"
      onSubmit={(values) =>
        createRoom.mutateAsync({
          data: {
            type: values.type,
            name: values.name || undefined,
            memberIds: members.map((m) => m.userId),
          },
        })
      }
    >
      <FormField
        control={form.control}
        name="type"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Type</FormLabel>
            <Select onValueChange={field.onChange} value={field.value}>
              <FormControl>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                {Object.values(ChatRoomType).map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="name"
        render={({ field }) => (
          <FormItem>
            <FormLabel>Name</FormLabel>
            <FormControl>
              <Input placeholder="Optional room name" {...field} />
            </FormControl>
          </FormItem>
        )}
      />
      <div className="grid gap-2">
        <FormLabel>Members</FormLabel>
        <UserMultiPicker value={members} onChange={setMembers} placeholder="Search people…" />
      </div>
    </FormDialog>
  );
}
