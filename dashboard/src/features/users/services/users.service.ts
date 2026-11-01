import { apiClient } from "@/lib/api/api-client";
import type { ExportFormat } from "@/components/export-menu";
import type { UserSummary } from "@/features/users/types";

export interface UserInput {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  additionalPhoneNumbers: string[];
  roleIds: string[];
  // Both sent only by a global actor; a non-global actor's users stay in their own current country.
  isGlobal?: boolean;
  countryIds?: string[];
}

const blankToNull = (value: string) => value.trim() || null;

function toBody({ firstName, lastName, phoneNumber, additionalPhoneNumbers, roleIds, isGlobal, countryIds }: UserInput) {
  return { firstName: blankToNull(firstName), lastName: blankToNull(lastName), phoneNumber: blankToNull(phoneNumber), additionalPhoneNumbers, roleIds, isGlobal, countryIds };
}

export const usersService = {
  list: () => apiClient<UserSummary[]>("/users"),

  create: (email: string, input: UserInput) =>
    apiClient<UserSummary>("/users", { method: "POST", body: { email: email.trim(), ...toBody(input) } }),

  update: (id: string, input: UserInput) =>
    apiClient<UserSummary>(`/users/${id}`, { method: "PUT", body: toBody(input) }),

  disable: (id: string, reason: string) => apiClient<UserSummary>(`/users/${id}/disable`, { method: "POST", body: { reason } }),
  enable: (id: string) => apiClient<UserSummary>(`/users/${id}/enable`, { method: "POST" }),
  lock: (id: string, reason: string) => apiClient<UserSummary>(`/users/${id}/lock`, { method: "POST", body: { reason } }),
  remove: (id: string) => apiClient(`/users/${id}`, { method: "DELETE" }),
  unlock: (id: string) => apiClient<UserSummary>(`/users/${id}/unlock`, { method: "POST" }),
  resetMfa: (id: string) => apiClient<UserSummary>(`/users/${id}/reset-mfa`, { method: "POST" }),
  resendInvitation: (id: string) => apiClient(`/users/${id}/resend-invitation`, { method: "POST" }),
  exportPath: (format: ExportFormat) => `/users/export?format=${format}`,
};
