import { apiClient } from "@/lib/api/api-client";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { ExportFormat } from "@/components/export-menu";
import type { Earnings, EarningsDriver } from "@/features/payments/types";

export interface EarningsPeriod {
  from: string;
  to: string;
}

// Everything collected since the start: the totals by month, and what each driver paid (the whole list, sorted by the table).
export const earningsService = {
  summary: ({ from, to }: EarningsPeriod) => apiClient<Earnings>(`/payments/earnings?${queryString({ from, to })}`),
  drivers: ({ from, to }: EarningsPeriod, q: string) => apiClient<Page<EarningsDriver>>(`/payments/earnings/drivers?${queryString({ from, to, q: q.trim(), size: 1000, sort: "totalPaid,desc" })}`),
  exportPath: ({ from, to }: EarningsPeriod, format: ExportFormat) => `/payments/earnings/export?${queryString({ from, to, format })}`,
};
