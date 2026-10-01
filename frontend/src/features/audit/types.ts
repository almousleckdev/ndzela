import type { Country } from "@/features/auth/types";

export interface AuditLog {
  id: string;
  createdAt: string;
  actor: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  target: string;
  result: "SUCCESS" | "FAILURE";
  reason: string | null;
  details: string | null;
  country: Country | null;
}

export interface AuditFilters {
  action: string;
  actor: string;
  targetType: string;
  from: string;
  to: string;
}
