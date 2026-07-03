import {
  BookOpen,
  CalendarClock,
  ClipboardList,
  Contact,
  FileText,
  FolderOpen,
  GraduationCap,
  LayoutDashboard,
  Library,
  type LucideIcon,
  Megaphone,
  MessageSquare,
  Bell as NotificationIcon,
  ShieldCheck,
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
  group: NavGroupKey;
}

export type NavGroupKey = "learning" | "communication" | "management";

export const NAV_GROUPS: readonly NavGroupKey[] = ["learning", "communication", "management"];

// Single source of truth for the app sidebar. Hrefs are locale-relative (the i18n <Link> adds the
// /{locale} prefix). Labels live in messages/*.json under `nav.<key>`.
export const NAV_ITEMS: readonly NavItem[] = [
  {
    key: "dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
    roles: [],
    group: "learning",
  },
  { key: "myCourses", href: "/dashboard/my-courses", icon: Library, roles: [], group: "learning" },
  {
    key: "schedule",
    href: "/dashboard/schedule",
    icon: CalendarClock,
    roles: [],
    group: "learning",
  },
  {
    key: "homework",
    href: "/dashboard/homework",
    icon: ClipboardList,
    roles: [],
    group: "learning",
  },
  { key: "tests", href: "/dashboard/tests", icon: FileText, roles: [], group: "learning" },
  {
    key: "gradebook",
    href: "/dashboard/gradebook",
    icon: GraduationCap,
    roles: [],
    group: "learning",
  },
  {
    key: "announcements",
    href: "/dashboard/announcements",
    icon: Megaphone,
    roles: [],
    group: "communication",
  },
  { key: "chat", href: "/dashboard/chat", icon: MessageSquare, roles: [], group: "communication" },
  {
    key: "notifications",
    href: "/dashboard/notifications",
    icon: NotificationIcon,
    roles: [],
    group: "communication",
  },
  { key: "files", href: "/dashboard/files", icon: FolderOpen, roles: [], group: "communication" },
  { key: "courses", href: "/dashboard/courses", icon: BookOpen, roles: [], group: "management" },
  {
    key: "groups",
    href: "/dashboard/groups",
    icon: Users,
    roles: [Role.ADMIN, Role.TEACHER],
    group: "management",
  },
  { key: "people", href: "/dashboard/people", icon: Contact, roles: [], group: "management" },
  {
    key: "admin",
    href: "/dashboard/admin",
    icon: ShieldCheck,
    roles: [Role.ADMIN],
    group: "management",
  },
] as const;

/** The nav items the current user may see, filtered by their realm roles. */
export function useNavItems(): readonly NavItem[] {
  const roles = useRoles();
  return NAV_ITEMS.filter(
    (item) => item.roles.length === 0 || item.roles.some((role) => roles.includes(role)),
  );
}

export function useNavGroups(): readonly { key: NavGroupKey; items: readonly NavItem[] }[] {
  const items = useNavItems();
  return NAV_GROUPS.map((key) => ({
    key,
    items: items.filter((item) => item.group === key),
  })).filter((group) => group.items.length > 0);
}
