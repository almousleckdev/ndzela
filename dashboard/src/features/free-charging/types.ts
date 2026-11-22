export type AgreementStatus = "ACTIVE" | "SUSPENDED" | "REVOKED";

export type EligibilityReason =
  | "ELIGIBLE"
  | "NO_AGREEMENT"
  | "AGREEMENT_SUSPENDED"
  | "AGREEMENT_NOT_STARTED"
  | "AGREEMENT_ENDED"
  | "OUTSIDE_WINDOW"
  | "UNPAID_DAYS"
  | "DRIVER_SUSPENDED"
  | "NOT_LINKED";

export interface Agreement {
  id: string;
  driverId: string;
  driverName: string;
  startsOn: string;
  endsOn: string | null;
  status: AgreementStatus;
  conditions: string | null;
  eligibility: EligibilityReason;
  unpaidDays: number;
  lastFreeUse: string | null;
}

export interface AgreementInput {
  startsOn: string;
  endsOn: string | null;
  conditions: string | null;
}

export interface FreeChargingUse {
  sessionId: string;
  driverId: string;
  driverName: string;
  startedAt: string;
  energyKwh: number | null;
  stopReason: string | null;
}

export type Weekday = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

export interface FreeWindow {
  day: Weekday;
  from: string;
  until: string;
  endOfDay: boolean;
}

export interface FreeWindowInput {
  day: Weekday;
  from: string;
  until: string | null;
  endOfDay: boolean;
}
