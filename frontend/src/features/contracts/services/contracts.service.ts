import { apiClient } from "@/lib/api/api-client";
import type { Contract, ContractTerms } from "@/features/contracts/types";

export const contractsService = {
  preview: (vehicleId: string, deposit: number) => apiClient<ContractTerms>("/contracts/preview", { method: "POST", body: { vehicleId, deposit } }),

  get: (id: string) => apiClient<Contract>(`/contracts/${id}`),

  pdfPath: (id: string) => `/contracts/${id}/pdf`,
};
