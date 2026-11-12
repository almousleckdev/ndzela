export type ArrearsLevel = "NONE" | "LATE" | "YELLOW" | "DARK_YELLOW" | "RED";
export type EntryType = "PAYMENT" | "REVERSAL" | "WAIVER" | "PENALTY" | "FEE";
export type PaymentMethod = "CASH" | "AIRTEL_MONEY" | "MOOV_MONEY" | "ORANGE_MONEY" | "OTHER" | "ZELLA_WALLET";
export type DayStatus = "PAID_ON_TIME" | "PAID_LATE" | "UNPAID";

// A rent day (or penalty) a payment settled
export interface PaidDay {
  day: string;
  kind: "RENT" | "PENALTY";
  amount: number;
  onTime: boolean;
}

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
  days: PaidDay[];
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

// Where a contract stands since day one: rent days (Monday to Saturday) and the money against the total price.
export interface ContractProgress {
  totalDays: number;
  elapsedDays: number;
  daysLeft: number;
  paidDays: number;
  unpaidDays: number;
  totalPrice: number;
  paidAmount: number;
  remainingAmount: number;
  deposit: number;
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
  // Sunday at noon: the week's rents must be settled by then.
  weekClosesAt: string;
  weekSettled: boolean;
  // Null without an active contract.
  progress: ContractProgress | null;
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
  // When the payment was recorded, and the days it settled.
  paidAt: string | null;
  days: PaidDay[];
}

export type IncidentKind = "HALF_DAY" | "STOP";
export type IncidentCause = "ELECTRICITY" | "LATE_SIGNING" | "OTHER";

export interface IncidentInput {
  day: string;
  kind: IncidentKind;
  cause: IncidentCause;
  hours?: number;
  note?: string;
}

export interface IncidentRow {
  id: string;
  day: string;
  kind: IncidentKind;
  cause: IncidentCause;
  hours: number | null;
  note: string | null;
  createdBy: string;
  createdAt: string;
  cancelledAt: string | null;
}

export type RiderScope = "CURRENT" | "FORMER";

export interface TransactionQuery {
  status: TransactionStatus | "ALL";
  driverId?: string;
  method: PaymentMethod | "ALL";
  scope?: RiderScope;
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

export interface CaseDriver {
  id: string;
  name: string;
  vehicle: string | null;
}

// A refused attempt to help a suspended driver. The helper is a driver only when that account is a linked NDZela driver; otherwise only the reference Zella gave.
export interface SuspensionCase {
  id: string;
  occurredAt: string;
  kind: string;
  amountMinor: number;
  currency: string | null;
  suspended: CaseDriver | null;
  helper: CaseDriver | null;
  helperReference: string;
  score: number | null;
  reasons: string[];
  flagged: boolean;
}

// An account of ours whose recent charges look like two cars while some drivers are suspended; nothing links it to one of them.
export interface PatternCase {
  account: CaseDriver;
  score: number;
  reasons: string[];
  suspendedDrivers: string[];
}
