"use client";

import { useMemo, useState } from "react";
import { Download, Plus, Zap } from "lucide-react";
import { format } from "date-fns";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { None, Stack } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { KpiBand, Page, PageHead, Panel, Toolbar, type Kpi } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { describeRange, NO_RANGE, rangeOf } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { cn } from "@/lib/utils";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { ExportDialog } from "@/features/charging/components/export-dialog";
import { RecordRechargeDialog } from "@/features/charging/components/record-recharge-dialog";
import { StationsDialog } from "@/features/charging/components/stations-dialog";
import { chargingService } from "@/features/charging/services/charging.service";
import type { Recharge } from "@/features/charging/types";
import { METHOD_LABELS } from "@/features/payments/lib/payment-labels";

const col = columnsFor<Recharge>();

function ChargingContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const query = useTableQuery({ range: rangeOf("today"), sorting: [{ id: "createdAt", desc: true }] });
  const { range } = query;
  const list = useAsync(() => chargingService.recharges({ q: query.q, from: range.from, to: range.to }, query.page, query.size, query.sort), [query.q, range.from, range.to, query.page, query.size, query.sort]);
  const summary = useAsync(chargingService.summary, []);
  const [recording, setRecording] = useState(false);
  const [stationsOpen, setStationsOpen] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [cancelling, setCancelling] = useState<Recharge | null>(null);
  const refresh = () => { list.reload(); summary.reload(); };
  const s = summary.data;
  const multiDay = range.from !== range.to;

  const columns = useMemo(() => col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Heure", cell: (c) => <span className="whitespace-nowrap tabular-nums">{format(new Date(c.getValue()), multiDay ? "dd/MM HH:mm" : "HH:mm")}</span> }),
    col.accessor("vehicle", { id: "vehicle", header: "Véhicule", enableSorting: false, cell: (c) => <Stack primary={c.getValue() ?? <None />} secondary={c.row.original.vehicleId ? undefined : "Voiture publique"} /> }),
    col.accessor("driverName", { id: "driver", header: "Chauffeur", enableSorting: false, cell: (c) => c.getValue() ?? <None /> }),
    col.accessor("amount", { id: "amount", header: "Montant", cell: (c) => <Stack primary={<span className={c.row.original.reversed ? "text-muted line-through" : ""}>{formatFcfa(c.getValue())}</span>} secondary={c.row.original.reversed ? <span className="text-red-700">Annulée</span> : undefined} /> }),
    col.accessor("batteryAfter", { id: "batteryAfter", header: "Solde après", cell: (c) => (
      <div className="whitespace-nowrap"><span className="font-medium">{c.getValue()} %</span><div className={cn("text-xs", c.row.original.critical ? "font-medium text-red-700" : "text-muted")}>arrivé à {c.row.original.batteryBefore} %</div></div>
    ) }),
    col.accessor("recordedBy", { id: "recordedBy", header: "Gestionnaire", enableSorting: false }),
    col.accessor("station", { id: "station", header: "Station", enableSorting: false, cell: (c) => c.getValue() ?? <None /> }),
    col.display({ id: "reference", header: "Référence", cell: (c) => { const r = c.row.original; return <span className="block max-w-[12rem] truncate" title={r.reference ?? undefined}>{r.method ? METHOD_LABELS[r.method] : ""}{r.reference ? ` · ${r.reference}` : ""}</span>; } }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => can(PERMISSIONS.CHARGING_REVERSE) && !c.row.original.reversed
      ? <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" onClick={() => setCancelling(c.row.original)}>Annuler</Button> : null }),
  ]), [can, multiDay]);

  const kpis: Kpi[] = [
    { label: "Recharges du jour", value: s?.rechargesToday ?? "-" },
    { label: "FCFA du jour", value: s ? formatFcfa(s.fcfaToday) : "-" },
    { label: "FCFA de la semaine", value: s ? formatFcfa(s.fcfaWeek) : "-" },
    { label: "FCFA du mois", value: s ? formatFcfa(s.fcfaMonth) : "-" },
    { label: "Comptes critiques", value: s?.criticalAccounts ?? "-", hint: "Arrivés batterie basse aujourd'hui", tone: s && s.criticalAccounts > 0 ? "bad" : undefined },
    { label: "Régularisations", value: s?.regularisations ?? "-", hint: "Recharges annulées ce mois" },
    { label: "Gestionnaires", value: s?.managers ?? "-", hint: "Ont saisi aujourd'hui" },
  ];

  const active: ActiveFilter[] = [
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead
        title="Recharge énergie"
        description="Les recharges payées sur place aux stations NDZELA, par les chauffeurs et par le public."
        actions={(
          <>
            {can(PERMISSIONS.CHARGING_MANAGE) && <Button variant="outline" onClick={() => setStationsOpen(true)}><Zap className="mr-2 h-4 w-4" /> Stations</Button>}
            {can(PERMISSIONS.CHARGING_EXPORT) && <Button variant="outline" onClick={() => setExporting(true)}><Download className="mr-2 h-4 w-4" /> Exporter</Button>}
            {can(PERMISSIONS.CHARGING_CREATE) && <Button onClick={() => setRecording(true)}><Plus className="mr-2 h-4 w-4" /> Enregistrer une recharge</Button>}
          </>
        )}
      />
      <KpiBand items={kpis} />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher une recharge" placeholder="Plaque, chauffeur ou référence" value={query.search} onChange={query.setSearch} />
            <DateRangePicker value={range} onChange={query.setRange} />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(r) => r.id} numeric={["amount"]} rowMarker={(r) => (r.critical && !r.reversed ? "bg-red-500" : undefined)}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucune recharge" description="Aucune recharge sur cette période. Choisissez une autre période ou enregistrez la première recharge." />} />
      </Panel>

      {recording && <RecordRechargeDialog onClose={() => setRecording(false)} onSaved={() => { setRecording(false); query.setRange(rangeOf("today")); refresh(); }} />}
      {stationsOpen && <StationsDialog onClose={() => setStationsOpen(false)} />}
      {exporting && <ExportDialog day={range.to || format(new Date(), "yyyy-MM-dd")} onClose={() => setExporting(false)} />}
      {cancelling && (
        <ReasonDialog title="Annuler cette recharge ?" description={`${cancelling.vehicle} · ${formatFcfa(cancelling.amount)}. Une écriture d'annulation sera ajoutée : l'originale reste visible.`}
          confirmText="Annuler la recharge" destructive onClose={() => setCancelling(null)}
          onConfirm={async (reason) => {
            await chargingService.reverse(cancelling.id, reason);
            setCancelling(null);
            addToast({ title: "Succès", description: "Recharge annulée.", type: "success" });
            refresh();
          }} />
      )}
    </Page>
  );
}

export function ChargingPage() {
  return (
    <RequirePermission permission={PERMISSIONS.CHARGING_READ}>
      <ChargingContent />
    </RequirePermission>
  );
}
