import { apiClient } from "@/lib/api/api-client";
import type { ExportFormat } from "@/components/export-menu";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { AccountDetail, PatternCase, SuspensionCase, AccountFilter, AccountRow, HistoryQuery, IncidentInput, LedgerEntry, PaymentMethod, PaymentResult, PaymentSummary, TransactionQuery, TransactionRow } from "@/features/payments/types";

const historyParams = ({ driverId, type, method, q, from, to }: HistoryQuery) => ({ driverId, type: type === "ALL" ? undefined : type, method: method === "ALL" ? undefined : method, q: q?.trim(), from, to });

export interface AccountDates {
  lateFrom?: string;
  lateTo?: string;
  paidFrom?: string;
  paidTo?: string;
}

export interface RecordPaymentInput {
  driverId: string;
  amount: number;
  method: PaymentMethod;
  reference?: string;
  // Generated once per form
  requestId: string;
}

export const paymentsService = {
  suspensionPatterns: () => apiClient<PatternCase[]>("/charging/suspension-cases/patterns"),

  suspensionCases: (page: number, size: number) => apiClient<Page<SuspensionCase>>(`/charging/suspension-cases?${queryString({ page, size })}`),

  summary: (range?: { from?: string; to?: string }) => apiClient<PaymentSummary>(`/payments/summary?${queryString({ from: range?.from, to: range?.to })}`),

  accounts: (filter: AccountFilter, q: string, page: number, size: number, sort?: string, dates: AccountDates = {}) =>
    apiClient<Page<AccountRow>>(`/payments/accounts?${queryString({ filter, q: q.trim(), sort, page, size, ...dates })}`),

  accountsExportPath: (filter: AccountFilter, q: string, dates: AccountDates, format: ExportFormat) =>
    `/payments/accounts/export?${queryString({ filter, q: q.trim(), format, ...dates })}`,

  account: (driverId: string) => apiClient<AccountDetail>(`/payments/accounts/${driverId}`),

  history: (query: HistoryQuery, page: number, size: number, sort?: string) => apiClient<Page<LedgerEntry>>(`/payments/history?${queryString({ ...historyParams(query), sort, page, size })}`),

  exportPath: (query: HistoryQuery, format: ExportFormat) => `/payments/export?${queryString({ ...historyParams(query), format })}`,

  transactions: (query: TransactionQuery, page: number, size: number) =>
    apiClient<Page<TransactionRow>>(`/payments/transactions?${queryString({ status: query.status === "ALL" ? undefined : query.status, method: query.method === "ALL" ? undefined : query.method, q: query.q.trim(), scope: query.scope, driverId: query.driverId, from: query.from, to: query.to, page, size })}`),

  // The ticket receipt (PDF) of a recorded payment
  receiptPath: (id: string) => `/payments/${id}/receipt`,

  // The mobile-money operators of the country
  operators: () => apiClient<{ code: PaymentMethod; label: string }[]>("/payments/operators"),

  declareIncident: (driverId: string, input: IncidentInput) => apiClient<void>(`/drivers/${driverId}/incidents`, { method: "POST", body: input }),

  record: (input: RecordPaymentInput) => apiClient<PaymentResult>("/payments", { method: "POST", body: input }),

  reverse: (entryId: string, reason: string) => apiClient<PaymentResult>(`/payments/${entryId}/reverse`, { method: "POST", body: { reason } }),

  waive: (driverId: string, amount: number, reason: string) => apiClient<PaymentResult>("/arrears/waivers", { method: "POST", body: { driverId, amount, reason } }),
};
