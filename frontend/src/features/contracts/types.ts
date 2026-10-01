export interface ContractTerms {
  startDate: string;
  endDate: string;
  billableDays: number;
  dailyRent: number;
  deposit: number;
  totalPrice: number;
}

export interface Contract extends ContractTerms {
  id: string;
  contractNumber: string;
  status: "ACTIVE" | "TERMINATED" | "COMPLETED";
  driverId: string;
  vehicleId: string;
  revision: number;
  updatedAt: string | null;
  createdAt: string;
  endedAt: string | null;
  endReason: string | null;
}
