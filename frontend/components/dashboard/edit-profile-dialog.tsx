"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useForm } from "react-hook-form";
import type { z } from "zod";

import { FormDialog } from "@/components/shared/form-dialog";
import { FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useGetMyProfile, useUpdateMyProfile } from "@/lib/api/user/user/user";
import { UpdateMyProfileBody } from "@/lib/api/user/zod/user/user.zod";

type FormValues = z.infer<typeof UpdateMyProfileBody>;

/** Edit the caller's own profile (display name + bio) — opened from the user menu. */
export function EditProfileDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("profile");
  const tc = useTranslations("common");
  const { data: profile } = useGetMyProfile();
  const form = useForm<FormValues>({
    resolver: zodResolver(UpdateMyProfileBody),
    values: { displayName: profile?.displayName ?? "", bio: profile?.bio ?? "" },
  });
  const update = useUpdateMyProfile();

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={t("title")}
      description={t("description")}
      form={form}
      success={t("saved")}
      submitLabel={tc("save")}
      pendingLabel={t("saving")}
      onSubmit={(values) => update.mutateAsync({ data: values })}
    >
      <FormField
        control={form.control}
        name="displayName"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("displayName")}</FormLabel>
            <FormControl>
              <Input {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
      <FormField
        control={form.control}
        name="bio"
        render={({ field }) => (
          <FormItem>
            <FormLabel>{t("bio")}</FormLabel>
            <FormControl>
              <Textarea placeholder={t("bioPlaceholder")} {...field} value={field.value ?? ""} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    </FormDialog>
  );
}
