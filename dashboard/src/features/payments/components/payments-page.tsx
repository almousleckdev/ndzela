"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { ExportMenu } from "@/components/export-menu";
import { KpiBand, Page, PageHead, Panel, Toolbar, type Kpi } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { usePolling } from "@/hooks/use-polling";
import { describeRange, NO_RANGE, rangeOf } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { LEVEL_MARKER, useAccountColumns } from "@/features/payments/components/account-columns";
import { RecordPaymentDialog } from "@/features/payments/components/record-payment-dialog";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { AccountFilter, AccountRow } from "@/features/payments/types";

const LEVELS = [
  { value: "UP_TO_DATE", label: "À jour" },
  { value: "LATE", label: "En retard (tous niveaux)" },
  { value: "YELLOW", label: "Rappel · 1 jour" },
  { value: "DARK_YELLOW", label: "Urgent · 2 jours" },
  { value: "RED", label: "Suspendu · 3 jours et +" },
];

function PaymentsContent() {
  const { can } = usePermissions();
  const query = useTableQuery({ range: rangeOf("last7"), sorting: [{ id: "driverName", desc: false }] });
  const { filters, range } = query;
  const filter = (filters.level || "ALL") as AccountFilter;
  const list = useAsync(() => paymentsService.accounts(filter, query.q, query.page, query.size, query.sort, { paidFrom: range.from, paidTo: range.to }), [filter, query.q, range.from, range.to, query.page, query.size, query.sort]);
  const summary = useAsync(() => paymentsService.summary({ from: range.from, to: range.to }), [range.from, range.to]);
  const [paying, setPaying] = useState<AccountRow | null>(null);
  const columns = useAccountColumns({ onPay: can(PERMISSIONS.PAYMENTS_CREATE) ? setPaying : undefined, showCollected: range.preset !== "all" });
  usePolling(() => { list.reload(); summary.reload(); }, 15000);
  const s = summary.data;

  const kpis: Kpi[] = [
    { label: range.preset === "all" ? "Encaissé (tout)" : `Encaissé · ${describeRange(range).toLowerCase()}`, value: s ? formatFcfa(s.collectedRange ?? s.collectedToday) : "-", hint: "Paiements non annulés" },
    { label: "Encaissé aujourd'hui", value: s ? formatFcfa(s.collectedToday) : "-" },
    { label: "Total impayé", value: s ? formatFcfa(s.totalOverdue) : "-", tone: s && s.totalOverdue > 0 ? "bad" : undefined },
    { label: "Chauffeurs à jour", value: s?.upToDate ?? "-" },
    { label: "En retard", value: s?.late ?? "-", hint: s ? `${s.red} à 3 jours ou plus` : undefined, tone: s && s.late > 0 ? "bad" : undefined, href: "/finance/arrears" },
  ];
  const active: ActiveFilter[] = [
    ...(filters.level ? [{ key: "level", label: LEVELS.find((l) => l.value === filters.level)?.label ?? filters.level, onRemove: () => query.setFilter("level", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: `Encaissé : ${describeRange(range)}`, onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead title="Suivi paiements" description="Qui a payé son loyer, qui est en retard et depuis quand. L'échéance de chaque jour est le lendemain à 12 h."
        actions={can(PERMISSIONS.PAYMENTS_EXPORT) && (
          <ExportMenu path={(format) => paymentsService.accountsExportPath(filter, query.q, { paidFrom: range.from, paidTo: range.to }, format)} filename="comptes-chauffeurs" failure="Impossible d'exporter les comptes." />
        )} />
      <KpiBand items={kpis} />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un chauffeur" placeholder="Nom, email ou téléphone" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Niveau" value={filters.level ?? ""} options={LEVELS} onChange={(value) => query.setFilter("level", value)} />
            <DateRangePicker value={range} onChange={query.setRange} label="Période d'encaissement" />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.driverId} numeric={["dailyRent", "overdueAmount", "collected"]} initiallyHidden={["dailyRent", "lastPaymentAt"]} rowMarker={(row) => LEVEL_MARKER[row.level]}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun chauffeur" description="Aucun chauffeur ne correspond à ces filtres. Effacez un filtre ou changez la période." />} />
      </Panel>
      {paying && <RecordPaymentDialog driverId={paying.driverId} driverName={paying.driverName} overdueAmount={paying.overdueAmount} dailyRent={paying.dailyRent} onClose={() => setPaying(null)} onSaved={() => { setPaying(null); list.reload(); summary.reload(); }} />}
    </Page>
  );
}

export function PaymentsPage() {
  return (
    <RequirePermission permission={PERMISSIONS.PAYMENTS_READ}>
      <PaymentsContent />
    </RequirePermission>
  );
}
