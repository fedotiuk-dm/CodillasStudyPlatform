export const SITE_NAME = "Codillas Study Platform";
export const SITE_SHORT_NAME = "Codillas";

/** Realm role names (mirror the backend's de.codillas.shared.security.Role). */
export const Role = {
  ADMIN: "ADMIN",
  TEACHER: "TEACHER",
  STUDENT: "STUDENT",
} as const;

export type Role = (typeof Role)[keyof typeof Role];
