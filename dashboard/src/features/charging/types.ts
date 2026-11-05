import type { PaymentMethod } from "@/features/payments/types";


export interface Station {
  id: string;
  name: string;
}

export interface VehicleOption {
  id: string;
  label: string;
  driverId: string;
  driverName: string;
}

export interface Recharge {
  id: string;
  type: "RECHARGE" | "REVERSAL";
  vehicleId: string | null;
  vehicle: string | null;
  driverId: string | null;
  driverName: string | null;
  publicLabel: string | null;
  stationId: string;
  station: string | null;
  amount: number;
  method: PaymentMethod | null;
  reference: string | null;
  batteryBefore: number | null;
  batteryAfter: number | null;
  kwh: number | null;
  critical: boolean;
  reversed: boolean;
  reason: string | null;
  recordedBy: string;
  createdAt: string;
  sessionId: string | null;
}

export interface ChargingSummary {
  rechargesToday: number;
  fcfaToday: number;
  fcfaWeek: number;
  fcfaMonth: number;
  criticalAccounts: number;
  regularisations: number;
  managers: number;
}

export interface SessionSummary {
  sessionsToday: number;
  fcfaToday: number;
  fcfaWeek: number;
  fcfaMonth: number;
}

export interface RecordRechargeInput {
  vehicleId?: string;
  publicLabel?: string;
  stationId: string;
  amount: number;
  method: PaymentMethod;
  reference?: string;
  batteryBefore: number;
  batteryAfter: number;
  kwh?: number;
  // The network session this pays for, when recorded from one
  sessionId?: string;
  // Generated once per form
  requestId: string;
}

// One recharge as the OCPP platform reports it: its amount is the platform's own (before VAT), not what was paid.
export interface ChargingSessionRow {
  id: string;
  transactionId: string;
  chargePointId: string | null;
  connectorId: string | null;
  idTag: string | null;
  startedAt: string | null;
  endedAt: string | null;
  status: string | null;
  energyKwh: number | null;
  amount: number | null;
  currency: string | null;
  stopReason: string | null;
  socBefore: number | null;
  socAfter: number | null;
  durationSeconds: number | null;
  pricePerKwh: number | null;
  powerKw: number | null;
  // The recharge that recorded its payment
  rechargeId: string | null;
  // The driver who charged and the car they held then; null for a public customer
  holder: SessionHolder | null;
  // What the driver paid from the Zella wallet; null when nothing was reported
  payment: SessionPayment | null;
}

export type PaymentState = "PENDING" | "CHARGED" | "NOT_CHARGED" | "UNDER_REVIEW" | "REFUNDED";

export interface SessionPayment {
  state: PaymentState | null;
  total: number | null;
  energyCost: number | null;
  vat: number | null;
  serviceFee: number | null;
  currency: string | null;
  method: string | null;
  breakdownEstimated: boolean;
  // The charge was limited to the authorised budget: the parts add up to more than the total
  capped: boolean;
}

export interface SessionHolder {
  driverId: string;
  driverName: string;
  vehicleId: string | null;
  vehicle: string | null;
}

export type IdTagStatus = "LINKED" | "SUGGESTED" | "UNLINKED";

export interface IdTagRow {
  id: string;
  // Null until the customer's first charge
  idTag: string | null;
  customerId: string;
  status: IdTagStatus;
  matchedOn: string;
  zellaName: string;
  driverId: string;
  driverName: string | null;
  driverEmail: string | null;
  driverPhone: string | null;
  nameMatches: boolean;
  decidedBy: string;
  decidedAt: string;
}
