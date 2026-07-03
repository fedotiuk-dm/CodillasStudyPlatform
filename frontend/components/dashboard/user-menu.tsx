"use client";

import { LogOut, UserPen } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

import { EditProfileDialog } from "@/components/dashboard/edit-profile-dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useKeycloak } from "@/lib/auth";

function initials(name: string | undefined): string {
  if (!name) return "?";
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? "")
    .join("");
}

export function UserMenu() {
  const tc = useTranslations("common");
  const tp = useTranslations("profile");
  const { name, email, roles, logout } = useKeycloak();
  const [editOpen, setEditOpen] = useState(false);

  return (
    <>
      <EditProfileDialog open={editOpen} onOpenChange={setEditOpen} />
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="rounded-full" aria-label={tc("userMenu")}>
            <Avatar>
              <AvatarFallback>{initials(name)}</AvatarFallback>
            </Avatar>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>
            <div className="flex flex-col">
              <span className="truncate text-sm font-medium">{name ?? "—"}</span>
              <span className="text-muted-foreground truncate text-xs">
                {email ?? roles.join(", ")}
              </span>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={() => setEditOpen(true)}>
            <UserPen className="size-4" />
            {tp("menuItem")}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => void logout()}>
            <LogOut className="size-4" />
            {tc("signOut")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}
