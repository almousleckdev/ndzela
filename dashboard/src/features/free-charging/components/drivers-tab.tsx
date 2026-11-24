"use client";

import { useMemo, useState } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { AgreementEditDialog } from "@/features/free-charging/components/agreement-edit-dialog";
import { AgreementStats } from "@/features/free-charging/components/agreement-stats";
import { driverColumns, type DriverAction } from "@/features/free-charging/components/driver-columns";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { Agreement, AgreementInput, DriverOption, Membership } from "@/features/free-charging/types";

const DONE: Record<"add" | "suspend" | "resume" | "revoke", string> = {
  add: "Chauffeur ajouté au programme.",
  suspend: "Accord suspendu.",
  resume: "Accord repris.",
  revoke: "Chauffeur retiré. L'historique est conservé.",
};

const MEMBERSHIPS = [
  { value: "IN_PROGRAM", label: "Dans le programme" },
  { value: "NOT_IN_PROGRAM", label: "Hors programme" },
];

const today = () => new Date().toISOString().slice(0, 10);

// The active drivers; a click adds one to the program or removes them.
export function DriversTab() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const canManage = can(PERMISSIONS.FREE_CHARGING_MANAGE);
  const query = useTableQuery();
  const membership = (query.filters.membership || "ALL") as Membership;
  const drivers = useAsync(() => freeChargingService.drivers(query.q, membership, query.page, query.size), [query.q, membership, query.page, query.size]);
  const agreements = useAsync(freeChargingService.list, []);
  const [busy, setBusy] = useState<Set<string>>(new Set());
  const [editing, setEditing] = useState<Agreement | null>(null);
  const reload = () => { drivers.reload(); agreements.reload(); };

  const run = async (kind: keyof typeof DONE, driver: DriverOption) => {
    setBusy((current) => new Set(current).add(driver.driverId));
    try {
      const id = driver.agreementId ?? "";
      if (kind === "add") await freeChargingService.create(driver.driverId, { startsOn: today(), endsOn: null, conditions: null });
      else await freeChargingService[kind](id);
      addToast({ title: "Succès", description: DONE[kind], type: "success" });
      reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
    } finally {
      setBusy((current) => { const next = new Set(current); next.delete(driver.driverId); return next; });
    }
  };

  const byId = useMemo(() => new Map((agreements.data ?? []).map((agreement) => [agreement.id, agreement])), [agreements.data]);
  const onAction = (action: DriverAction, driver: DriverOption) => {
    if (action === "edit") setEditing(byId.get(driver.agreementId ?? "") ?? null);
    else void run(action, driver);
  };
  const columns = useMemo(() => driverColumns({ canManage, agreements: byId, busy, onAction }), [canManage, byId, busy]); // eslint-disable-line react-hooks/exhaustive-deps

  const saveEdit = async (input: AgreementInput) => {
    if (!editing) return;
    await freeChargingService.update(editing.id, input);
    addToast({ title: "Succès", description: "Accord modifié.", type: "success" });
    setEditing(null);
    reload();
  };

  return (
    <div className="space-y-4">
      {agreements.data && agreements.data.length > 0 && <AgreementStats items={agreements.data} />}
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un chauffeur" placeholder="Nom, téléphone, e-mail ou voiture" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Programme" value={query.filters.membership ?? ""} options={MEMBERSHIPS} onChange={(value) => query.setFilter("membership", value)} />
          </>
        )} />
        <DataTable columns={columns} data={drivers.data?.items ?? null} error={drivers.error} onRetry={drivers.reload} getRowId={(row) => row.driverId}
          server={{ total: drivers.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun chauffeur actif" description="Aucun chauffeur actif ne correspond à cette recherche." />} />
        {editing && <AgreementEditDialog agreement={editing} onClose={() => setEditing(null)} onSubmit={saveEdit} />}
      </Panel>
    </div>
  );
}
