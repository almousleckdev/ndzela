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
  additionalPhoneNumbers: string[];
  profilePictureUrl: string | null;
  status: UserStatus;
  statusReason: string | null;
  mfaEnabled: boolean;
  roles: RoleRef[];
  // Sees every country, not only the ones in `countries` (which is then empty).
  isGlobal: boolean;
  countries: Country[];
  createdAt: string;
}
