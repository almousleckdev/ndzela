"use client";

import { useCallback, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Eye, MoreHorizontal, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ColorLabel } from "@/components/ui/color-picker";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { EmptyState } from "@/components/ui/empty-state";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { Stack, Status, type Tone } from "@/components/data-table/cells";
import { BulkDelete } from "@/components/data-table/bulk-delete";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { KpiBand, Page, PageHead, Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { ExportMenu } from "@/components/export-menu";
import { useToast } from "@/hooks/use-toast";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { CountryFlag } from "@/features/tenancy/components/country-flag";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import { STATUS_ACTIONS, STATUS_LABELS } from "@/features/vehicles/lib/vehicle-status";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";
import { VehicleFormDialog } from "@/features/vehicles/components/vehicle-form-dialog";
import type { Vehicle, VehicleStatus } from "@/features/vehicles/types";

const TONE: Record<VehicleStatus, Tone> = { AVAILABLE: "good", ASSIGNED: "info", IN_MAINTENANCE: "watch", ACCIDENT: "bad", SUSPENDED: "alert", OUT_OF_SERVICE: "neutral" };
const col = columnsFor<Vehicle>();

function VehiclesContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const router = useRouter();
  const isGlobal = useAuthStore((state) => state.user?.country === null);
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const query = useTableQuery({ sorting: [{ id: "fleetNumber", desc: false }] });
  const { filters, range } = query;
  const status = (filters.status || "ALL") as VehicleStatus | "ALL";
  const list = useAsync(() => vehiclesService.list({ status, q: query.q, from: range.from, to: range.to, sort: query.sort, page: query.page, size: query.size }), [status, query.q, range.from, range.to, query.sort, query.page, query.size]);
  const counts = useAsync(vehiclesService.counts, []);
  const [editing, setEditing] = useState<Vehicle | "new" | null>(null);
  const [changing, setChanging] = useState<{ vehicle: Vehicle; action: (typeof STATUS_ACTIONS)[number] } | null>(null);
  const [deleting, setDeleting] = useState<Vehicle | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const { reload: reloadList } = list;
  const { reload: reloadCounts } = counts;
  const refresh = useCallback(() => { reloadList(); reloadCounts(); }, [reloadList, reloadCounts]);
  const mustPickCountry = isGlobal && selectedCountry === null;
  const total = counts.data ? Object.values(counts.data).reduce((sum, value) => sum + value, 0) : 0;

  const run = useCallback(async (request: () => Promise<unknown>, success: string, failure: string) => {
    try {
      await request();
      addToast({ title: "Succès", description: success, type: "success" });
      refresh();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, failure), type: "error" });
    }
  }, [addToast, refresh]);

  const columns = useMemo(() => col.columns([
    col.accessor("fleetNumber", { id: "fleetNumber", header: "N°", cell: (c) => <span className="font-medium">{c.getValue()}</span> }),
    col.accessor("brand", { id: "brand", header: "Véhicule", cell: (c) => <Stack primary={`${c.row.original.brand} ${c.row.original.model}`} secondary={<span className="inline-flex items-center gap-1.5">{c.row.original.year} · <ColorLabel value={c.row.original.color} /></span>} /> }),
    col.accessor("plate", { id: "plate", header: "Immatriculation" }),
    col.accessor("dailyRent", { id: "dailyRent", header: "Loyer / jour", cell: (c) => formatFcfa(c.getValue()) }),
    ...(isGlobal ? [col.display({ id: "country", header: "Pays", cell: (c) => c.row.original.country ? <span className="inline-flex items-center gap-2"><CountryFlag code={c.row.original.country.code} />{c.row.original.country.name}</span> : null })] : []),
    col.accessor("status", { id: "status", header: "Statut", cell: (c) => <Stack primary={<Status tone={TONE[c.getValue()]}>{STATUS_LABELS[c.getValue()]}</Status>} secondary={c.row.original.statusReason && c.getValue() !== "AVAILABLE" && c.getValue() !== "ASSIGNED" ? c.row.original.statusReason : undefined} /> }),
    col.accessor("createdAt", { id: "createdAt", header: "Ajouté le", cell: (c) => <span className="whitespace-nowrap tabular-nums">{new Date(c.getValue()).toLocaleDateString("fr-FR")}</span> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const vehicle = c.row.original;
      return (
        <div className="flex justify-end gap-0.5">
          <Button variant="ghost" size="icon" aria-label={`Voir ${vehicle.fleetNumber}`} title="Voir les détails" onClick={() => router.push(`/vehicles/${vehicle.id}`)}><Eye className="h-4 w-4" /></Button>
          {(can(PERMISSIONS.VEHICLES_UPDATE) || can(PERMISSIONS.VEHICLES_DELETE)) && (
            <Dropdown trigger={<Button variant="ghost" size="icon" aria-label={`Actions pour ${vehicle.fleetNumber}`}><MoreHorizontal className="h-4 w-4" /></Button>}>
              {can(PERMISSIONS.VEHICLES_UPDATE) && <DropdownItem onClick={() => setEditing(vehicle)}>Modifier</DropdownItem>}
              {can(PERMISSIONS.VEHICLES_UPDATE) && vehicle.status !== "ASSIGNED" && STATUS_ACTIONS.filter((action) => action.status !== vehicle.status).map((action) => (
                <DropdownItem key={action.status} onClick={() => (action.status === "AVAILABLE" ? run(() => vehiclesService.changeStatus(vehicle.id, "AVAILABLE"), "Véhicule remis disponible.", "Impossible de changer le statut.") : setChanging({ vehicle, action }))}>{action.label}</DropdownItem>
              ))}
              {can(PERMISSIONS.VEHICLES_DELETE) && vehicle.status !== "ASSIGNED" && <DropdownItem destructive onClick={() => setDeleting(vehicle)}>Supprimer</DropdownItem>}
            </Dropdown>
          )}
        </div>
      );
    } }),
  ]), [can, isGlobal, router, run]);

  const statusOptions = (["AVAILABLE", "ASSIGNED", "IN_MAINTENANCE", "OUT_OF_SERVICE", "ACCIDENT", "SUSPENDED"] as VehicleStatus[]).map((value) => ({ value, label: STATUS_LABELS[value], count: counts.data?.[value] }));
  const active: ActiveFilter[] = [
    ...(filters.status ? [{ key: "status", label: STATUS_LABELS[filters.status as VehicleStatus], onRemove: () => query.setFilter("status", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: `Ajouté : ${describeRange(range)}`, onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];
  const c = counts.data;

  return (
    <Page>
      <PageHead
        title="Véhicules"
        description="Les véhicules de la flotte, leur état et leur loyer journalier."
        actions={(
          <>
            {can(PERMISSIONS.VEHICLES_EXPORT) && <ExportMenu range={range} path={(format, c) => vehiclesService.exportPath({ status, q: query.q, from: c.range.from, to: c.range.to }, format)} filename="vehicules" failure="Impossible d'exporter les véhicules." />}
            {can(PERMISSIONS.VEHICLES_CREATE) && (
              <div className="text-right">
                <Button onClick={() => setEditing("new")} disabled={mustPickCountry}><Plus className="mr-2 h-4 w-4" /> Ajouter un véhicule</Button>
                {mustPickCountry && <p className="mt-1 text-xs text-muted">Choisissez un pays dans l&apos;en-tête.</p>}
              </div>
            )}
          </>
        )}
      />
      <KpiBand items={[
        { label: "Total véhicules", value: c ? total : "-" },
        { label: "Disponibles", value: c?.AVAILABLE ?? "-" },
        { label: "Loués", value: c?.ASSIGNED ?? "-" },
        { label: "Hors circulation", value: c ? c.IN_MAINTENANCE + c.ACCIDENT + c.SUSPENDED + c.OUT_OF_SERVICE : "-", hint: "Maintenance, accident, suspendus, hors service" },
      ]} />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un véhicule" placeholder="Numéro, plaque, marque ou modèle" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Statut" value={filters.status ?? ""} options={statusOptions} onChange={(value) => query.setFilter("status", value)} />
            <DateRangePicker value={range} onChange={query.setRange} label="Ajouté le" />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        {can(PERMISSIONS.VEHICLES_DELETE) && (
          <BulkDelete items={list.data?.items ?? []} selected={selected} onSelectedChange={setSelected} idOf={(v) => v.id} nameOf={(v) => `${v.fleetNumber} · ${v.plate}`}
            remove={(v) => vehiclesService.remove(v.id)} noun={{ one: "véhicule", many: "véhicules" }} note="Impossible pour un véhicule loué ou qui a de l'argent enregistré : retirez-le du chauffeur ou mettez-le hors service." onDone={refresh} />
        )}
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(vehicle) => vehicle.id} selection={can(PERMISSIONS.VEHICLES_DELETE) ? { selected, onChange: setSelected } : undefined} numeric={["dailyRent"]} initiallyHidden={["createdAt"]}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun véhicule" description={total === 0 ? "Ajoutez le premier véhicule de la flotte." : "Aucun véhicule ne correspond à ces filtres."} />} />
      </Panel>

      {editing && <VehicleFormDialog vehicle={editing === "new" ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); refresh(); }} />}
      {changing && (
        <ReasonDialog title={changing.action.title} description={`${changing.vehicle.fleetNumber} · ${changing.vehicle.plate}`} confirmText="Confirmer" onClose={() => setChanging(null)}
          onConfirm={async (reason) => {
            await vehiclesService.changeStatus(changing.vehicle.id, changing.action.status, reason);
            setChanging(null);
            addToast({ title: "Succès", description: "Statut mis à jour.", type: "success" });
            refresh();
          }} />
      )}
      <ConfirmDialog isOpen={deleting !== null} onClose={() => setDeleting(null)} title="Supprimer ce véhicule ?" confirmText="Supprimer" cancelText="Annuler" destructive
        description={deleting ? `${deleting.fleetNumber} (${deleting.plate}) sera supprimé définitivement avec ses anciens contrats et affectations. Un véhicule loué ne peut pas être supprimé, ni un véhicule lié à un paiement ou à une recharge.` : ""}
        onConfirm={() => { const target = deleting; setDeleting(null); if (target) run(() => vehiclesService.remove(target.id), "Véhicule supprimé.", "Impossible de supprimer le véhicule."); }} />
    </Page>
  );
}

export function VehiclesPage() {
  return (
    <RequirePermission permission={PERMISSIONS.VEHICLES_READ}>
      <VehiclesContent />
    </RequirePermission>
  );
}
