import { apiClient } from "@/lib/api/api-client";
import type { ExportFormat } from "@/components/export-menu";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { IdTagRow, IdTagStatus, ChargingSummary, SessionSummary, Recharge, RecordRechargeInput, Station, VehicleOption, ChargingSessionRow } from "@/features/charging/types";

export const chargingService = {
  summary: () => apiClient<ChargingSummary>("/charging/summary"),

  recharges: (range: { q?: string; from?: string; to?: string }, page: number, size: number, sort?: string) => apiClient<Page<Recharge>>(`/charging/recharges?${queryString({ q: range.q?.trim(), from: range.from, to: range.to, sort, page, size })}`),

  sessions: (query: { q?: string; status?: string; from?: string; to?: string }, page: number, size: number) =>
    apiClient<Page<ChargingSessionRow>>(`/charging/sessions?${queryString({ q: query.q?.trim(), status: query.status, from: query.from, to: query.to, page, size })}`),

  sessionSummary: () => apiClient<SessionSummary>("/charging/sessions/summary"),

  idTags: (status: IdTagStatus) => apiClient<IdTagRow[]>(`/charging/id-tags?status=${status}`),
  confirmIdTag: (id: string) => apiClient(`/charging/id-tags/${id}/confirm`, { method: "POST" }),
  unlinkIdTag: (id: string, reason: string) => apiClient(`/charging/id-tags/${id}/unlink`, { method: "POST", body: { reason } }),
  lookupIdTags: () => apiClient<{ stored: number }>("/charging/id-tags/lookup", { method: "POST" }),

  record: (input: RecordRechargeInput) => apiClient<Recharge>("/charging/recharges", { method: "POST", body: input }),

  reverse: (id: string, reason: string) => apiClient<Recharge>(`/charging/recharges/${id}/reverse`, { method: "POST", body: { reason } }),

  vehicles: (q = "") => apiClient<VehicleOption[]>(`/charging/vehicles${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ""}`),

  exportPath: ({ q, from, to }: { q: string; from: string; to: string }, format: ExportFormat) => `/charging/export?${queryString({ q: q.trim(), from, to, format })}`,
  exportSessionsPath: ({ q, from, to }: { q: string; from: string; to: string }, format: ExportFormat) => `/charging/sessions/export?${queryString({ q: q.trim(), from, to, format })}`,

  stations: () => apiClient<Station[]>("/charging/stations"),
  createStation: (name: string) => apiClient<Station>("/charging/stations", { method: "POST", body: { name } }),
  updateStation: (id: string, name: string) => apiClient<Station>(`/charging/stations/${id}`, { method: "PUT", body: { name } }),
  removeStation: (id: string) => apiClient(`/charging/stations/${id}`, { method: "DELETE" }),
};
