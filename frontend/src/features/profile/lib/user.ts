import type { UserProfile } from "@/features/auth/types";

/** "First Last", or the email when the user has not filled in a name. */
export function displayName(user: Pick<UserProfile, "firstName" | "lastName" | "email">): string {
  const name = [user.firstName, user.lastName].filter(Boolean).join(" ");
  return name || user.email;
}
