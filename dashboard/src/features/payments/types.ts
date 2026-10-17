export type ArrearsLevel = "NONE" | "LATE" | "YELLOW" | "DARK_YELLOW" | "RED";
export type EntryType = "PAYMENT" | "REVERSAL" | "WAIVER" | "PENALTY" | "FEE";
export type PaymentMethod = "CASH" | "AIRTEL_MONEY" | "MOOV_MONEY" | "ORANGE_MONEY" | "OTHER";
export type DayStatus = "PAID_ON_TIME" | "PAID_LATE" | "UNPAID";

export interface LedgerEntry {
  id: string;
  type: EntryType;
  amount: number;
  method: PaymentMethod | null;
  reference: string | null;
  reason: string | null;
  driverId: string;
  driverName: string | null;
  reversesEntryId: string | null;
  reversed: boolean;
  businessDay: string | null;
  recordedBy: string;
  createdAt: string;
}

export interface AccountRow {
  driverId: string;
  driverName: string;
  phone: string;
  driverStatus: string;
  contractId: string | null;
  contractNumber: string | null;
  dailyRent: number | null;
  vehicle: string | null;
  level: ArrearsLevel;
  overdueAmount: number;
  // Rent of the day that ended but whose deadline (noon) has not come yet.
  awaitingDays: number;
  awaitingAmount: number;
  overdueSince: string | null;
  lastPaymentAt: string | null;
  // Money received in the chosen period; null when no period is chosen.
  collected: number | null;
  // Rent days paid, unpaid (already due), elapsed since the contract started, in all, and still to come.
  paidDays: number;
  unpaidDays: number;
  elapsedDays: number;
  totalDays: number;
  daysLeft: number;
}

export interface DayView {
  day: string;
  dueAt: string;
  amount: number;
  status: DayStatus;
  settledAt: string | null;
}

export interface AccountDetail {
  driverId: string;
  driverName: string;
  driverStatus: string;
  contractId: string | null;
  contractNumber: string | null;
  dailyRent: number | null;
  level: ArrearsLevel;
  overdueAmount: number;
  credit: number;
  overdueSince: string | null;
  paidOnTime: number;
  paidLate: number;
  unpaid: number;
  days: DayView[];
  entries: LedgerEntry[];
}

export interface PaymentResult {
  entry: LedgerEntry;
  level: ArrearsLevel;
  overdueAmount: number;
  credit: number;
}

export interface PaymentSummary {
  collectedToday: number;
  collectedRange: number | null;
  totalOverdue: number;
  totalAwaiting: number;
  // Riders who reach 3 unpaid days at noon if yesterday's rent is not paid.
  atRisk: number;
  upToDate: number;
  late: number;
  yellow: number;
  darkYellow: number;
  red: number;
}

export type TransactionStatus = "SUCCESS" | "FAILED" | "PENDING" | "EXPIRED" | "REVERSED";

// A payment made from the driver app (PVIT) or entered by staff (MANUAL).
export interface TransactionRow {
  id: string;
  createdAt: string;
  driverId: string;
  driverName: string;
  phone: string | null;
  amount: number;
  method: PaymentMethod;
  status: TransactionStatus;
  reference: string;
  providerReference: string | null;
  source: "PVIT" | "MANUAL";
}

export type IncidentKind = "HALF_DAY" | "STOP";
export type IncidentCause = "ELECTRICITY" | "OTHER";

export interface IncidentInput {
  day: string;
  kind: IncidentKind;
  cause: IncidentCause;
  hours?: number;
  note?: string;
}

export interface TransactionQuery {
  status: TransactionStatus | "ALL";
  method: PaymentMethod | "ALL";
  q: string;
  from?: string;
  to?: string;
}

export type AccountFilter = "ALL" | "UP_TO_DATE" | "LATE" | "YELLOW" | "DARK_YELLOW" | "RED";

export interface HistoryQuery {
  driverId?: string;
  type: EntryType | "ALL";
  method: PaymentMethod | "ALL";
  q?: string;
  from: string;
  to: string;
}
