import { apiClient } from "@/lib/api/api-client";
import type { RoleSummary } from "@/features/roles/types";

export const rolesService = {
  /** Every role, to assign them when inviting or editing a user. */
  list: () => apiClient<RoleSummary[]>("/roles"),

  /** The roles the Roles page shows: all for role managers, otherwise only the caller's own. */
  listVisible: () => apiClient<RoleSummary[]>("/roles/visible"),

  /** Every permission that exists; roles can only be built from these. */
  permissions: () => apiClient<string[]>("/permissions"),

  create: (name: string, permissions: string[]) =>
    apiClient<RoleSummary>("/roles", { method: "POST", body: { name, permissions } }),

  updatePermissions: (id: string, permissions: string[]) =>
    apiClient<RoleSummary>(`/roles/${id}/permissions`, { method: "PUT", body: { permissions } }),

  remove: (id: string) => apiClient(`/roles/${id}`, { method: "DELETE" }),
};
