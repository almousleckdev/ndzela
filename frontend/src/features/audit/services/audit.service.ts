import { apiClient } from "@/lib/api/api-client";
import type { Page } from "@/lib/api/page";
import { queryString } from "@/lib/api/query";
import type { AuditFilters, AuditLog } from "@/features/audit/types";

export const auditService = {
  search: (filters: AuditFilters, page: number, size: number, sort?: string) =>
    apiClient<Page<AuditLog>>(`/audit?${queryString({ ...filters, actor: filters.actor.trim(), sort, page, size })}`),
};
