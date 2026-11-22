import { apiClient } from "@/lib/api/api-client";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import { DRIVER_PAGE_SIZE, type Membership } from "@/features/free-charging/lib/selection";
import type { Agreement, AgreementInput, DriverOption, FreeChargingUse, FreeWindow, FreeWindowInput } from "@/features/free-charging/types";

export const freeChargingService = {
  list: () => apiClient<Agreement[]>("/free-charging/agreements"),

  create: (driverId: string, input: AgreementInput) => apiClient<Agreement>("/free-charging/agreements", { method: "POST", body: { driverId, ...input } }),

  update: (id: string, input: AgreementInput) => apiClient<Agreement>(`/free-charging/agreements/${id}`, { method: "PUT", body: input }),

  suspend: (id: string) => apiClient<Agreement>(`/free-charging/agreements/${id}/suspend`, { method: "POST" }),

  resume: (id: string) => apiClient<Agreement>(`/free-charging/agreements/${id}/resume`, { method: "POST" }),

  revoke: (id: string) => apiClient<Agreement>(`/free-charging/agreements/${id}/revoke`, { method: "POST" }),

  drivers: (q: string, membership: Membership, page: number) =>
    apiClient<Page<DriverOption>>(`/free-charging/drivers?${queryString({ q, membership, page, size: DRIVER_PAGE_SIZE })}`),

  window: () => apiClient<FreeWindow>("/free-charging/window"),

  changeWindow: (input: FreeWindowInput) => apiClient<FreeWindow>("/free-charging/window", { method: "PUT", body: input }),

  usage: (page: number, size: number) => apiClient<Page<FreeChargingUse>>(`/free-charging/usage?${queryString({ page, size })}`),
};
