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
}
