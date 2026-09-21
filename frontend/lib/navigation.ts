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

import { usePrimaryRole } from "@/lib/auth";
import { Role } from "@/lib/constants";

export interface NavItem {
  /** Stable key + i18n message key under the `nav` namespace. */
  key: string;
  href: string;
  icon: LucideIcon;
  /** Primary roles (the highest role held) that see the item; empty means everyone. */
  roles: readonly Role[];
  group: NavGroupKey;
}

export type NavGroupKey = "learning" | "communication" | "management";

export const NAV_GROUPS: readonly NavGroupKey[] = ["learning", "communication", "management"];

const STAFF = [Role.ADMIN, Role.TEACHER] as const;

// Single source of truth for the app sidebar. Hrefs are locale-relative (the i18n <Link> adds the
// /{locale} prefix). Labels live in messages/*.json under `nav.<key>`. The same route may appear
// twice under different keys so each role gets its own label/section (my-courses, courses).
export const NAV_ITEMS: readonly NavItem[] = [
  { key: "dashboard", href: "/dashboard", icon: LayoutDashboard, roles: [], group: "learning" },
  {
    key: "myCourses",
    href: "/dashboard/my-courses",
    icon: Library,
    roles: [Role.STUDENT],
    group: "learning",
  },
  {
    key: "myGroups",
    href: "/dashboard/my-courses",
    icon: Library,
    roles: [Role.TEACHER],
    group: "learning",
  },
  {
    key: "schedule",
    href: "/dashboard/schedule",
    icon: CalendarClock,
    roles: [Role.STUDENT, Role.TEACHER],
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
    key: "catalog",
    href: "/dashboard/courses",
    icon: BookOpen,
    roles: [Role.STUDENT],
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
  { key: "courses", href: "/dashboard/courses", icon: BookOpen, roles: STAFF, group: "management" },
  { key: "groups", href: "/dashboard/groups", icon: Users, roles: STAFF, group: "management" },
  { key: "people", href: "/dashboard/people", icon: Contact, roles: STAFF, group: "management" },
  {
    key: "admin",
    href: "/dashboard/admin",
    icon: ShieldCheck,
    roles: [Role.ADMIN],
    group: "management",
  },
] as const;

/** The nav items the current user may see, by their primary (highest) role. */
export function useNavItems(): readonly NavItem[] {
  const role = usePrimaryRole();
  return NAV_ITEMS.filter((item) => item.roles.length === 0 || item.roles.includes(role));
}

export function useNavGroups(): readonly { key: NavGroupKey; items: readonly NavItem[] }[] {
  const items = useNavItems();
  return NAV_GROUPS.map((key) => ({
    key,
    items: items.filter((item) => item.group === key),
  })).filter((group) => group.items.length > 0);
}
