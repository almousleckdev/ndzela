import type { Country } from "@/features/auth/types";

export type VehicleStatus = "AVAILABLE" | "ASSIGNED" | "IN_MAINTENANCE" | "ACCIDENT" | "SUSPENDED" | "OUT_OF_SERVICE";

export interface Vehicle {
  id: string;
  fleetNumber: string;
  brand: string;
  model: string;
  year: number;
  plate: string;
  color: string;
  vin: string | null;
  purchasePrice: number;
  dailyRent: number;
  status: VehicleStatus;
  statusReason: string | null;
  country: Country | null;
  createdAt: string;
}

export interface VehicleImage {
  id: string;
  createdAt: string;
}

export type VehicleCounts = Record<VehicleStatus, number>;

export interface VehicleQuery {
  status: VehicleStatus | "ALL";
  q: string;
  from?: string;
  to?: string;
  sort?: string;
  page: number;
  size: number;
}
