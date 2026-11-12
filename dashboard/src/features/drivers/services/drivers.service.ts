import { apiClient } from "@/lib/api/api-client";
import type { ExportFormat } from "@/components/export-menu";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { Contract } from "@/features/contracts/types";
import type { Driver, DriverFileKind, DriverQuery, DriverStats, Leave, LeaveInput } from "@/features/drivers/types";
import type { Uploads } from "@/features/drivers/lib/driver-form";

// The JSON of a driver's details (the wizard builds it; phones are already international).
export type DriverDetailsBody = Record<string, unknown>;

export const driversService = {
  list: ({ status, scope, onLeave, late, q, from, to, sort, page, size }: DriverQuery) => apiClient<Page<Driver>>(`/drivers?${queryString({ status: status === "ALL" ? undefined : status, scope, onLeave, late, q: q.trim(), from, to, sort, page, size })}`),

  stats: () => apiClient<DriverStats>("/drivers/stats"),

  exportPath: ({ status, onLeave, late, q, from, to }: Pick<DriverQuery, "status" | "onLeave" | "late" | "q" | "from" | "to">, format: ExportFormat) => `/drivers/export?${queryString({ status: status === "ALL" ? undefined : status, onLeave, late, q: q.trim(), from, to, format })}`,

  leaves: (id: string) => apiClient<Leave[]>(`/drivers/${id}/leaves`),

  declareLeave: (id: string, input: LeaveInput) => apiClient<Leave>(`/drivers/${id}/leaves`, { method: "POST", body: input }),

  cancelLeave: (id: string, leaveId: string) => apiClient(`/drivers/${id}/leaves/${leaveId}/cancel`, { method: "POST" }),

  updateContract: (id: string, deposit: number) => apiClient<Contract>(`/drivers/${id}/contract`, { method: "PUT", body: { deposit } }),

  deactivate: (id: string, reason: string) => apiClient<Driver>(`/drivers/${id}/deactivate`, { method: "POST", body: { reason } }),

  get: (id: string) => apiClient<Driver>(`/drivers/${id}`),

  create: (body: DriverDetailsBody, uploads: Uploads) => {
    const form = new FormData();
    form.append("data", new Blob([JSON.stringify(body)], { type: "application/json" }));
    for (const [key, file] of Object.entries(uploads)) if (file) form.append(key, file);
    return apiClient<Driver>("/drivers", { method: "POST", body: form });
  },

  update: (id: string, body: DriverDetailsBody) => apiClient<Driver>(`/drivers/${id}`, { method: "PUT", body }),

  suspend: (id: string, reason: string, releaseVehicle = false) => apiClient<Driver>(`/drivers/${id}/suspend`, { method: "POST", body: { reason, releaseVehicle } }),

  block: (id: string, reason: string) => apiClient<Driver>(`/drivers/${id}/block`, { method: "POST", body: { reason } }),

  reactivate: (id: string) => apiClient<Driver>(`/drivers/${id}/reactivate`, { method: "POST" }),

  unlockLogin: (id: string) => apiClient<Driver>(`/drivers/${id}/unlock-login`, { method: "POST" }),

  remove: (id: string) => apiClient(`/drivers/${id}`, { method: "DELETE" }),

  assignVehicle: (id: string, vehicleId: string, deposit: number) => apiClient<Driver>(`/drivers/${id}/vehicle`, { method: "POST", body: { vehicleId, deposit } }),

  changeVehicle: (id: string, vehicleId: string) => apiClient<Driver>(`/drivers/${id}/vehicle`, { method: "PUT", body: { vehicleId } }),

  changeContractStart: (id: string, startDate: string) => apiClient<Contract>(`/drivers/${id}/contract/start-date`, { method: "PUT", body: { startDate } }),

  removeVehicle: (id: string, reason: string) => apiClient<Driver>(`/drivers/${id}/vehicle/remove`, { method: "POST", body: { reason } }),

  filePath: (id: string, kind: DriverFileKind) => `/drivers/${id}/files/${kind}`,

  replaceFile: (id: string, kind: DriverFileKind, file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiClient<Driver>(`/drivers/${id}/files/${kind}`, { method: "PUT", body: form });
  },

  removeFile: (id: string, kind: DriverFileKind) => apiClient<Driver>(`/drivers/${id}/files/${kind}`, { method: "DELETE" }),
};
