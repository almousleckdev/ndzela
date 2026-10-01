"use client";

import { useMemo, useState } from "react";
import { Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { None, Stack, Status, type Tone } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Page, PageHead, Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { ENTRY_TYPE_LABELS, METHOD_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { EntryType, HistoryQuery, LedgerEntry, PaymentMethod } from "@/features/payments/types";

const TYPE_TONE: Record<EntryType, Tone> = { PAYMENT: "good", REVERSAL: "bad", WAIVER: "info", PENALTY: "watch" };
const TYPE_OPTIONS = Object.entries(ENTRY_TYPE_LABELS).map(([value, label]) => ({ value, label }));
const METHOD_OPTIONS = Object.entries(METHOD_LABELS).map(([value, label]) => ({ value, label }));
const col = columnsFor<LedgerEntry>();

function HistoryContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const query = useTableQuery({ sorting: [{ id: "createdAt", desc: true }] });
  const { filters, range } = query;
  const [reversing, setReversing] = useState<LedgerEntry | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  const params: HistoryQuery = { type: (filters.type || "ALL") as HistoryQuery["type"], method: (filters.method || "ALL") as HistoryQuery["method"], q: query.q, from: range.from, to: range.to };
  const list = useAsync(() => paymentsService.history(params, query.page, query.size, query.sort), [filters.type, filters.method, query.q, range.from, range.to, query.page, query.size, query.sort]);

  const exportCsv = async () => {
    setIsExporting(true);
    try {
      await downloadFile(paymentsService.exportPath(params), "paiements.csv");
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'exporter les paiements."), type: "error" });
    } finally {
      setIsExporting(false);
    }
  };

  const columns = useMemo(() => col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Date", cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.accessor("driverName", { id: "driver", header: "Chauffeur", enableSorting: false, cell: (c) => c.getValue() ?? <None /> }),
    col.accessor("type", { id: "type", header: "Type", cell: (c) => <Stack primary={<Status tone={TYPE_TONE[c.getValue()]}>{ENTRY_TYPE_LABELS[c.getValue()]}</Status>} secondary={c.row.original.reversed ? "Annulé" : undefined} /> }),
    col.accessor("amount", { id: "amount", header: "Montant", cell: (c) => <span className={c.row.original.reversed ? "text-muted line-through" : "font-medium"}>{formatFcfa(c.getValue())}</span> }),
    col.accessor("method", { id: "method", header: "Moyen", cell: (c) => (c.getValue() ? METHOD_LABELS[c.getValue() as PaymentMethod] : <None />) }),
    col.display({ id: "reference", header: "Référence ou motif", cell: (c) => <span className="block max-w-[16rem] truncate" title={c.row.original.reference ?? c.row.original.reason ?? undefined}>{c.row.original.reference ?? c.row.original.reason ?? <None />}</span> }),
    col.accessor("recordedBy", { id: "recordedBy", header: "Saisi par", enableSorting: false }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const entry = c.row.original;
      return can(PERMISSIONS.PAYMENTS_REVERSE) && (entry.type === "PAYMENT" || entry.type === "WAIVER") && !entry.reversed
        ? <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" onClick={() => setReversing(entry)}>Annuler</Button> : null;
    } }),
  ]), [can]);

  const active: ActiveFilter[] = [
    ...(filters.type ? [{ key: "type", label: ENTRY_TYPE_LABELS[filters.type as EntryType], onRemove: () => query.setFilter("type", "") }] : []),
    ...(filters.method ? [{ key: "method", label: METHOD_LABELS[filters.method as PaymentMethod], onRemove: () => query.setFilter("method", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead
        title="Historique des paiements"
        description="Toutes les écritures : paiements, annulations, délais accordés et pénalités. Elles ne peuvent ni être modifiées ni supprimées : une erreur se corrige par une annulation."
        actions={can(PERMISSIONS.PAYMENTS_EXPORT) && <Button variant="outline" onClick={exportCsv} isLoading={isExporting}><Download className="mr-2 h-4 w-4" /> Exporter</Button>}
      />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un paiement" placeholder="Chauffeur ou référence" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Type" value={filters.type ?? ""} options={TYPE_OPTIONS} onChange={(value) => query.setFilter("type", value)} />
            <FilterPill label="Moyen" value={filters.method ?? ""} options={METHOD_OPTIONS} onChange={(value) => query.setFilter("method", value)} />
            <DateRangePicker value={range} onChange={query.setRange} />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(entry) => entry.id} numeric={["amount"]}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucune écriture" description="Aucun paiement ne correspond à ces filtres. Élargissez la période ou effacez un filtre." />} />
      </Panel>

      {reversing && (
        <ReasonDialog title="Annuler cette écriture ?" description={`${ENTRY_TYPE_LABELS[reversing.type]} de ${formatFcfa(reversing.amount)} pour ${reversing.driverName ?? ""}. Une écriture d'annulation sera ajoutée : l'originale reste visible.`}
          confirmText="Annuler l'écriture" destructive onClose={() => setReversing(null)}
          onConfirm={async (reason) => {
            await paymentsService.reverse(reversing.id, reason);
            setReversing(null);
            addToast({ title: "Succès", description: "Écriture annulée.", type: "success" });
            list.reload();
          }} />
      )}
    </Page>
  );
}

export function HistoryPage() {
  return (
    <RequirePermission permission={PERMISSIONS.PAYMENTS_READ}>
      <HistoryContent />
    </RequirePermission>
  );
}
