import type { PaymentMethod } from "@/features/payments/types";

export type ChargingPeriod = "DAY" | "WEEK" | "MONTH" | "YEAR";

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
  /** Generated once per form: sending it twice records the recharge once. */
  requestId: string;
}
