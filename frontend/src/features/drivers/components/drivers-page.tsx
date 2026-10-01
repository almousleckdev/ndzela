"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { CalendarOff, Download, Eye, GraduationCap, MoreHorizontal, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { None, Stack, Status, type Tone } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { KpiBand, Page, PageHead, Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { DriverActionDialogs, availableActions, type DriverAction } from "@/features/drivers/components/driver-actions";
import { DriverAvatar } from "@/features/drivers/components/driver-avatar";
import { DriverLeavesDialog } from "@/features/drivers/components/driver-leaves-dialog";
import { TrainingCentersDialog } from "@/features/drivers/components/training-centers-dialog";
import { DRIVER_STATUS_LABELS, displayStatus } from "@/features/drivers/lib/driver-labels";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver, DriverQuery } from "@/features/drivers/types";
import { LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import { CountryFlag } from "@/features/tenancy/components/country-flag";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";

type Filter = "ACTIVE" | "ON_LEAVE" | "LATE" | "INACTIVE" | "SUSPENDED" | "BLOCKED";

/** What a choice asks the server for: "Actif" excludes drivers on leave, "En arrêt" and "En retard" are their own choices. */
function queryOf(filter: string): Pick<DriverQuery, "status" | "onLeave" | "late"> {
  switch (filter as Filter) {
    case "ON_LEAVE": return { status: "ACTIVE", onLeave: true };
    case "LATE": return { status: "ALL", late: true };
    case "ACTIVE": return { status: "ACTIVE", onLeave: false };
    case "INACTIVE": case "SUSPENDED": case "BLOCKED": return { status: filter as "INACTIVE" | "SUSPENDED" | "BLOCKED" };
    default: return { status: "ALL" };
  }
}

const TONE: Record<string, Tone> = { success: "good", warning: "watch", danger: "bad", neutral: "neutral", info: "info" };
const LEVEL_TONE = { NONE: "good", LATE: "neutral", YELLOW: "watch", DARK_YELLOW: "alert", RED: "bad" } as const;
const col = columnsFor<Driver>();

function DriversContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const router = useRouter();
  const isGlobal = useAuthStore((state) => state.user?.country === null);
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const query = useTableQuery({ sorting: [{ id: "lastName", desc: false }] });
  const { filters, range } = query;
  const server = queryOf(filters.status ?? "");
  const list = useAsync(() => driversService.list({ ...server, q: query.q, from: range.from, to: range.to, sort: query.sort, page: query.page, size: query.size }), [filters.status, query.q, range.from, range.to, query.sort, query.page, query.size]);
  const stats = useAsync(driversService.stats, []);
  const [action, setAction] = useState<DriverAction | null>(null);
  const [leavesOf, setLeavesOf] = useState<Driver | null>(null);
  const [centersOpen, setCentersOpen] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const mustPickCountry = isGlobal && selectedCountry === null;
  const refresh = () => { list.reload(); stats.reload(); };
  const s = stats.data;

  const exportCsv = async () => {
    setIsExporting(true);
    try {
      await downloadFile(driversService.exportPath({ ...server, q: query.q, from: range.from, to: range.to }), "chauffeurs.csv");
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'exporter les chauffeurs."), type: "error" });
    } finally {
      setIsExporting(false);
    }
  };

  const columns = useMemo(() => col.columns([
    col.accessor((d) => `${d.personal.lastName} ${d.personal.firstName}`, { id: "lastName", header: "Chauffeur", cell: (c) => (
      <div className="flex items-center gap-3">
        <DriverAvatar driver={c.row.original} size="sm" />
        <Stack primary={<Link href={`/drivers/${c.row.original.id}`} className="hover:text-primary-ink">{c.getValue()}</Link>} secondary={c.row.original.personal.email} />
      </div>
    ) }),
    col.accessor((d) => d.personal.phonePrimary, { id: "phone", header: "Téléphone", cell: (c) => <span className="whitespace-nowrap tabular-nums">{c.getValue()}</span> }),
    col.display({ id: "vehicle", header: "Véhicule", cell: (c) => (c.row.original.vehicle ? `${c.row.original.vehicle.fleetNumber} · ${c.row.original.vehicle.plate}` : <span className="text-muted">Aucun</span>) }),
    ...(isGlobal ? [col.display({ id: "country", header: "Pays", cell: (c) => c.row.original.country ? <span className="inline-flex items-center gap-2"><CountryFlag code={c.row.original.country.code} />{c.row.original.country.name}</span> : <None /> })] : []),
    col.accessor("status", { id: "status", header: "Statut", cell: (c) => {
      const d = c.row.original;
      const shown = displayStatus(d);
      const secondary = d.arrears.level !== "NONE" ? <span className="inline-flex items-center gap-1.5"><Status tone={LEVEL_TONE[d.arrears.level]}>{LEVEL_LABELS[d.arrears.level]}</Status></span> : d.statusReason && d.status !== "ACTIVE" ? d.statusReason : undefined;
      return <Stack primary={<Status tone={TONE[shown.variant]}>{shown.label}</Status>} secondary={secondary} />;
    } }),
    col.accessor("createdAt", { id: "createdAt", header: "Inscrit le", cell: (c) => <span className="whitespace-nowrap tabular-nums">{new Date(c.getValue()).toLocaleDateString("fr-FR")}</span> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const driver = c.row.original;
      const name = `${driver.personal.lastName} ${driver.personal.firstName}`;
      return (
        <div className="flex justify-end gap-0.5">
          <Button variant="ghost" size="icon" aria-label={`Voir ${name}`} title="Voir les détails" onClick={() => router.push(`/drivers/${driver.id}`)}><Eye className="h-4 w-4" /></Button>
          <Button variant="ghost" size="icon" aria-label={`Gérer les arrêts de ${name}`} title="Gérer les arrêts" onClick={() => setLeavesOf(driver)}><CalendarOff className="h-4 w-4" /></Button>
          <Dropdown trigger={<Button variant="ghost" size="icon" aria-label={`Actions pour ${name}`}><MoreHorizontal className="h-4 w-4" /></Button>}>
            <DropdownItem onClick={() => router.push(`/drivers/${driver.id}`)}>Voir</DropdownItem>
            {can(PERMISSIONS.DRIVERS_UPDATE) && <DropdownItem onClick={() => router.push(`/drivers/${driver.id}/edit`)}>Modifier</DropdownItem>}
            {availableActions(driver, can).map((item) => <DropdownItem key={item.kind} destructive={item.destructive} onClick={() => setAction({ kind: item.kind, driver })}>{item.label}</DropdownItem>)}
          </Dropdown>
        </div>
      );
    } }),
  ]), [can, isGlobal, router]);

  const options = [
    { value: "ACTIVE", label: DRIVER_STATUS_LABELS.ACTIVE, count: s?.active },
    { value: "ON_LEAVE", label: "En arrêt", count: s?.onLeave },
    { value: "LATE", label: "En retard", count: s?.late },
    { value: "INACTIVE", label: DRIVER_STATUS_LABELS.INACTIVE, count: s?.inactive },
    { value: "SUSPENDED", label: DRIVER_STATUS_LABELS.SUSPENDED, count: s?.suspended },
    { value: "BLOCKED", label: DRIVER_STATUS_LABELS.BLOCKED, count: s?.blocked },
  ];
  const active: ActiveFilter[] = [
    ...(filters.status ? [{ key: "status", label: options.find((o) => o.value === filters.status)?.label ?? filters.status, onRemove: () => query.setFilter("status", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: `Inscrit : ${describeRange(range)}`, onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead
        title="Chauffeurs"
        description="Les chauffeurs de la flotte et le véhicule qu'ils conduisent."
        actions={(
          <>
            {can(PERMISSIONS.TRAINING_CENTERS_MANAGE) && <Button variant="outline" onClick={() => setCentersOpen(true)}><GraduationCap className="mr-2 h-4 w-4" /> Centres de formation</Button>}
            {can(PERMISSIONS.DRIVERS_EXPORT) && <Button variant="outline" onClick={exportCsv} isLoading={isExporting}><Download className="mr-2 h-4 w-4" /> Exporter</Button>}
            {can(PERMISSIONS.DRIVERS_CREATE) && (
              <div className="text-right">
                {mustPickCountry ? <Button disabled><Plus className="mr-2 h-4 w-4" /> Nouveau chauffeur</Button> : <Link href="/drivers/new"><Button><Plus className="mr-2 h-4 w-4" /> Nouveau chauffeur</Button></Link>}
                {mustPickCountry && <p className="mt-1 text-xs text-muted">Choisissez un pays dans l&apos;en-tête.</p>}
              </div>
            )}
          </>
        )}
      />
      <KpiBand items={[
        { label: "Total chauffeurs", value: s?.total ?? "-" },
        { label: "Actifs", value: s?.active ?? "-" },
        { label: "En arrêt", value: s?.onLeave ?? "-" },
        { label: "En retard", value: s?.late ?? "-", tone: s && s.late > 0 ? "bad" : undefined },
        { label: "Suspendus", value: s?.suspended ?? "-" },
        { label: "Assignés / sans véhicule", value: s ? `${s.assigned} / ${s.withoutVehicle}` : "-" },
      ]} />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un chauffeur" placeholder="Nom, email ou téléphone" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Statut" value={filters.status ?? ""} options={options} onChange={(value) => query.setFilter("status", value)} />
            <DateRangePicker value={range} onChange={query.setRange} label="Inscrit le" />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(driver) => driver.id} initiallyHidden={["createdAt"]}
          rowMarker={(driver) => ({ NONE: undefined, LATE: "bg-slate-400", YELLOW: "bg-yellow-400", DARK_YELLOW: "bg-orange-500", RED: "bg-red-600" } as const)[driver.arrears.level]}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun chauffeur" description={(s?.total ?? 0) === 0 ? "Créez le premier chauffeur." : "Aucun chauffeur ne correspond à ces filtres."} />} />
      </Panel>

      <DriverActionDialogs target={action} onClose={() => setAction(null)} onDone={() => { setAction(null); refresh(); }} />
      {leavesOf && <DriverLeavesDialog driver={leavesOf} canDeclare={can(PERMISSIONS.DRIVERS_UPDATE)} onClose={() => setLeavesOf(null)} onChanged={refresh} />}
      {centersOpen && <TrainingCentersDialog onClose={() => setCentersOpen(false)} />}
    </Page>
  );
}

export function DriversPage() {
  return (
    <RequirePermission permission={PERMISSIONS.DRIVERS_READ}>
      <DriversContent />
    </RequirePermission>
  );
}
