"use client";

import { useCallback, useState } from "react";
import { TriangleAlert } from "lucide-react";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { ExportMenu } from "@/components/export-menu";
import { KpiBand, Page, PageHead, Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { usePolling } from "@/hooks/use-polling";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { LEVEL_MARKER, useAccountColumns } from "@/features/payments/components/account-columns";
import { IncidentDialog } from "@/features/payments/components/incident-dialog";
import { PatternCasesPanel } from "@/features/payments/components/pattern-cases-panel";
import { SuspensionCasesPanel } from "@/features/payments/components/suspension-cases-panel";
import { RecordPaymentDialog } from "@/features/payments/components/record-payment-dialog";
import { WaiverDialog } from "@/features/payments/components/waiver-dialog";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { AccountFilter, AccountRow, IncidentKind } from "@/features/payments/types";

const LEVELS = [
  { value: "YELLOW", label: "Rappel · 1 jour" },
  { value: "DARK_YELLOW", label: "Urgent · 2 jours" },
  { value: "RED", label: "Suspendu · 3 jours et +" },
];

function ArrearsContent() {
  const { can } = usePermissions();
  const query = useTableQuery();
  const { filters, range } = query;
  const filter = (filters.level || "LATE") as AccountFilter;
  const list = useAsync(() => paymentsService.accounts(filter, query.q, query.page, query.size, query.sort, { lateFrom: range.from, lateTo: range.to }), [filter, query.q, range.from, range.to, query.page, query.size, query.sort]);
  const summary = useAsync(() => paymentsService.summary(), []);
  const [paying, setPaying] = useState<AccountRow | null>(null);
  const [waiving, setWaiving] = useState<AccountRow | null>(null);
  const [declaring, setDeclaring] = useState<{ row: AccountRow; kind: IncidentKind } | null>(null);
  const onIncident = useCallback((row: AccountRow, kind: IncidentKind) => setDeclaring({ row, kind }), []);
  const columns = useAccountColumns({ onPay: can(PERMISSIONS.PAYMENTS_CREATE) ? setPaying : undefined, onWaive: can(PERMISSIONS.ARREARS_MANAGE) ? setWaiving : undefined, onIncident: can(PERMISSIONS.ARREARS_MANAGE) ? onIncident : undefined, contact: true, showCollected: false });
  const refresh = () => { list.reload(); summary.reload(); };
  usePolling(refresh, 15000);
  const s = summary.data;

  const active: ActiveFilter[] = [
    ...(filters.level ? [{ key: "level", label: LEVELS.find((l) => l.value === filters.level)?.label ?? filters.level, onRemove: () => query.setFilter("level", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: `En retard : ${describeRange(range)}`, onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead title="Gérer impayés" description="Les chauffeurs dont le loyer n'est pas réglé, du plus ancien au plus récent. À 3 jours d'impayés le chauffeur est suspendu automatiquement."
        actions={can(PERMISSIONS.PAYMENTS_EXPORT) && (
          <ExportMenu range={range} path={(format, c) => paymentsService.accountsExportPath(filter, query.q, { lateFrom: c.range.from, lateTo: c.range.to }, format)} filename="comptes-impayes" failure="Impossible d'exporter les comptes." />
        )} />
      <KpiBand items={[
        { label: "Total impayé", value: s ? formatFcfa(s.totalOverdue) : "-", tone: s && s.totalOverdue > 0 ? "bad" : undefined },
        { label: "À régler avant midi", value: s ? formatFcfa(s.totalAwaiting) : "-" },
        { label: "À recouvrer (hier inclus)", value: s ? formatFcfa(s.totalOverdue + s.totalAwaiting) : "-" },
        { label: "Chauffeurs en retard", value: s?.late ?? "-" },
        { label: "Rappel · 1 jour", value: s?.yellow ?? "-" },
        { label: "Urgent · 2 jours", value: s?.darkYellow ?? "-", tone: s && s.darkYellow > 0 ? "bad" : undefined },
        { label: "Suspendu · 3 jours et +", value: s?.red ?? "-", tone: s && s.red > 0 ? "bad" : undefined },
      ]} />

      {s && s.red > 0 && (
        <div role="alert" className="flex items-start gap-3 rounded-lg border border-red-300 bg-red-100 px-4 py-3 text-sm text-red-950">
          <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-900" />
          <p><strong>{s.red} chauffeur(s) ont dépassé le délai de 3 jours d&apos;impayés.</strong> Ils sont suspendus automatiquement. Bloquez leur véhicule dans l&apos;outil GPS : le blocage à distance n&apos;est pas géré par cette plateforme.</p>
        </div>
      )}

      {s && s.atRisk > 0 && (
        <div role="status" className="flex items-start gap-3 rounded-lg border border-yellow-300 bg-yellow-50 px-4 py-3 text-sm text-yellow-950">
          <TriangleAlert className="mt-0.5 h-5 w-5 shrink-0 text-yellow-700" />
          <p><strong>{s.atRisk} chauffeur(s) passeront à 3 jours d&apos;impayés à midi</strong> si le loyer d&apos;hier n&apos;est pas réglé. Ils seront alors suspendus automatiquement.</p>
        </div>
      )}

      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un chauffeur" placeholder="Nom, email ou téléphone" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Niveau" value={filters.level ?? ""} options={LEVELS} onChange={(value) => query.setFilter("level", value)} />
            <DateRangePicker value={range} onChange={query.setRange} label="En retard depuis" />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.driverId} numeric={["dailyRent", "overdueAmount"]} initiallyHidden={["dailyRent"]} rowMarker={(row) => LEVEL_MARKER[row.level]}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun impayé" description="Tous les chauffeurs de cette sélection sont à jour." />} />
      </Panel>

      {can(PERMISSIONS.ARREARS_MANAGE) && <SuspensionCasesPanel />}
      {can(PERMISSIONS.ARREARS_MANAGE) && <PatternCasesPanel />}

      {paying && <RecordPaymentDialog driverId={paying.driverId} driverName={paying.driverName} overdueAmount={paying.overdueAmount} dailyRent={paying.dailyRent} onClose={() => setPaying(null)} onSaved={() => { setPaying(null); refresh(); }} />}
      {waiving && <WaiverDialog driverId={waiving.driverId} driverName={waiving.driverName} overdueAmount={waiving.overdueAmount} dailyRent={waiving.dailyRent} onClose={() => setWaiving(null)} onSaved={() => { setWaiving(null); refresh(); }} />}
      {declaring && <IncidentDialog driverId={declaring.row.driverId} driverName={declaring.row.driverName} kind={declaring.kind} onClose={() => setDeclaring(null)} onSaved={() => { setDeclaring(null); refresh(); }} />}
    </Page>
  );
}

export function ArrearsPage() {
  return (
    <RequirePermission permission={PERMISSIONS.PAYMENTS_READ}>
      <ArrearsContent />
    </RequirePermission>
  );
}
