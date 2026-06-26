"use client";

import { useTranslations } from "next-intl";
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
  onCreated?: () => void;
}) {
  const t = useTranslations("chat");
  const form = useForm<FormValues>({
    defaultValues: { type: ChatRoomType.GROUP, name: "" },
  });
  const createRoom = useCreateRoom();
  const [members, setMembers] = useState<{ userId: string; displayName: string }[]>([]);

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("newRoom")}
      description={t("dialogDescription")}
      form={form}
      onCreated={onCreated}
      onReset={() => setMembers([])}
      submitDisabled={members.length === 0}
      success={t("created")}
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
            <FormLabel>{t("type")}</FormLabel>
            <Select onValueChange={field.onChange} value={field.value}>
              <FormControl>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
              </FormControl>
              <SelectContent>
                {Object.values(ChatRoomType).map((roomType) => (
                  <SelectItem key={roomType} value={roomType}>
                    {roomType}
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
            <FormLabel>{t("name")}</FormLabel>
            <FormControl>
              <Input placeholder={t("namePlaceholder")} {...field} />
            </FormControl>
          </FormItem>
        )}
      />
      <div className="grid gap-2">
        <FormLabel>{t("members")}</FormLabel>
        <UserMultiPicker value={members} onChange={setMembers} placeholder={t("searchPeople")} />
      </div>
    </FormDialog>
  );
}
