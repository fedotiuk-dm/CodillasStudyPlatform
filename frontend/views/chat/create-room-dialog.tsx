"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

import { UserMultiPicker } from "@/components/shared/user-picker";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
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

  function onSubmit(values: FormValues) {
    if (members.length === 0) return;
    createRoom.mutate(
      {
        data: {
          type: values.type,
          name: values.name || undefined,
          memberIds: members.map((m) => m.userId),
        },
      },
      {
        onSuccess: () => {
          toast.success("Room created");
          onCreated();
          onOpenChange(false);
          form.reset();
          setMembers([]);
        },
        onError: () => toast.error("Could not create the room"),
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New room</DialogTitle>
          <DialogDescription>Start a conversation with one or more members.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
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
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="grid gap-2">
              <FormLabel>Members</FormLabel>
              <UserMultiPicker value={members} onChange={setMembers} placeholder="Search people…" />
            </div>
            <DialogFooter>
              <Button type="submit" disabled={createRoom.isPending || members.length === 0}>
                {createRoom.isPending ? "Creating…" : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
