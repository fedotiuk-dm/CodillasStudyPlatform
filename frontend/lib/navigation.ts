import {
  BookOpen,
  ClipboardList,
  FileText,
  FolderOpen,
  GraduationCap,
  type LucideIcon,
  MessageSquare,
  Bell as NotificationIcon,
  Users,
} from "lucide-react";

import { useRoles } from "@/lib/auth";
import { Role } from "@/lib/constants";

export interface NavItem {
  /** Stable key + i18n message key under the `nav` namespace. */
  key: string;
  href: string;
  icon: LucideIcon;
  /** Roles allowed to see the item; empty means any authenticated user. */
  roles: readonly Role[];
}

// Single source of truth for the app sidebar. Hrefs are locale-relative (the i18n <Link> adds the
// /{locale} prefix). Labels live in messages/*.json under `nav.<key>`.
export const NAV_ITEMS: readonly NavItem[] = [
  { key: "courses", href: "/dashboard/courses", icon: BookOpen, roles: [] },
  { key: "groups", href: "/dashboard/groups", icon: Users, roles: [Role.ADMIN, Role.TEACHER] },
  { key: "homework", href: "/dashboard/homework", icon: ClipboardList, roles: [] },
  { key: "tests", href: "/dashboard/tests", icon: FileText, roles: [] },
  { key: "gradebook", href: "/dashboard/gradebook", icon: GraduationCap, roles: [] },
  { key: "chat", href: "/dashboard/chat", icon: MessageSquare, roles: [] },
  { key: "notifications", href: "/dashboard/notifications", icon: NotificationIcon, roles: [] },
  { key: "files", href: "/dashboard/files", icon: FolderOpen, roles: [] },
] as const;

/** The nav items the current user may see, filtered by their realm roles. */
export function useNavItems(): readonly NavItem[] {
  const roles = useRoles();
  return NAV_ITEMS.filter(
    (item) => item.roles.length === 0 || item.roles.some((role) => roles.includes(role)),
  );
}
