import { apiClient } from "@/lib/api/api-client";
import type { RoleSummary } from "@/features/roles/types";

export const rolesService = {
  list: () => apiClient<RoleSummary[]>("/roles"),

  /** Every permission that exists; roles can only be built from these. */
  permissions: () => apiClient<string[]>("/permissions"),

  create: (name: string, permissions: string[]) =>
    apiClient<RoleSummary>("/roles", { method: "POST", body: { name, permissions } }),

  updatePermissions: (id: string, permissions: string[]) =>
    apiClient<RoleSummary>(`/roles/${id}/permissions`, { method: "PUT", body: { permissions } }),

  remove: (id: string) => apiClient(`/roles/${id}`, { method: "DELETE" }),
};
