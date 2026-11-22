"use client";

import { useMemo } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { None } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { columnsFor } from "@/components/data-table/features";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Panel } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { formatDateTime } from "@/lib/time";
import { reasonLabel } from "@/features/charging/lib/stop-reason";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { FreeChargingUse } from "@/features/free-charging/types";

const col = columnsFor<FreeChargingUse>();

export function UsageTab() {
  const query = useTableQuery();
  const list = useAsync(() => freeChargingService.usage(query.page, query.size), [query.page, query.size]);

  const columns = useMemo(() => col.columns([
    col.accessor("driverName", { id: "driver", header: "Chauffeur", enableSorting: false, cell: (c) => <span className="font-medium text-ink">{c.getValue()}</span> }),
    col.accessor("startedAt", { id: "startedAt", header: "Date", enableSorting: false, cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.accessor("energyKwh", { id: "energyKwh", header: "Énergie", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : <span className="whitespace-nowrap tabular-nums">{c.getValue()} kWh</span>) }),
    col.accessor("stopReason", { id: "stopReason", header: "Motif d'arrêt", enableSorting: false, cell: (c) => reasonLabel(c.getValue(), false) ?? <None /> }),
  ]), []);

  return (
    <Panel>
      <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.sessionId} numeric={["energyKwh"]}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: [], onSortingChange: () => {} }}
        empty={<EmptyState title="Aucune recharge gratuite" description="Aucune recharge gratuite n'a encore été utilisée." />} />
    </Panel>
  );
}
