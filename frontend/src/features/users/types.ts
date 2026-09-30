import type { Country } from "@/features/auth/types";

export type UserStatus = "ACTIVE" | "MFA_SETUP" | "LOCKED" | "DISABLED";

export interface RoleRef {
  id: string;
  name: string;
}

export interface UserSummary {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  phoneNumber: string | null;
  profilePictureUrl: string | null;
  status: UserStatus;
  mfaEnabled: boolean;
  roles: RoleRef[];
  country: Country | null;
  createdAt: string;
}
