export interface StaffAccess {
  linked: boolean;
  vehicleId: string | null;
  vehicle: string | null;
  plate: string | null;
  freeCharging: boolean;
}

export interface StaffAccessInput {
  vehicleId?: string;
  plate?: string;
  freeCharging: boolean;
}
