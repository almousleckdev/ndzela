"use client";

import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { DataTable } from "@/components/data-table/data-table";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { progressionColumns } from "@/features/payments/components/progression-columns";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { AccountFilter } from "@/features/payments/types";

const STATES = [{ value: "UP_TO_DATE", label: "À jour" }, { value: "LATE", label: "En retard" }];

// Each driver's progress through the contract, with the days paid and unpaid; filter by all
export function ProgressionTab() {
  const query = useTableQuery({ sorting: [{ id: "driverName", desc: false }] });
  const filter = (query.filters.state || "ALL") as AccountFilter;
  const list = useAsync(() => paymentsService.accounts(filter, query.q, query.page, query.size, query.sort, {}), [filter, query.q, query.page, query.size, query.sort]);
  const active: ActiveFilter[] = [
    ...(query.filters.state ? [{ key: "state", label: STATES.find((s) => s.value === query.filters.state)?.label ?? query.filters.state, onRemove: () => query.setFilter("state", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
  ];
  return (
    <>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher un chauffeur" placeholder="Nom, email ou téléphone" value={query.search} onChange={query.setSearch} />
          <FilterPill label="Situation" value={query.filters.state ?? ""} options={STATES} onChange={(value) => query.setFilter("state", value)} />
        </>
      )} />
      <ActiveFilters filters={active} onClear={query.reset} />
      <DataTable columns={progressionColumns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.driverId} numeric={["overdueAmount"]}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
        empty={<EmptyState title="Aucun chauffeur" description="Aucun chauffeur ne correspond à ces filtres." />} />
    </>
  );
}
