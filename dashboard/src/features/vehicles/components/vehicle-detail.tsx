"use client";

import { useState } from "react";
import Link from "next/link";
import { BackLink } from "@/components/layout/back-link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { ColorLabel } from "@/components/ui/color-picker";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { STATUS_ACTIONS, STATUS_LABELS, STATUS_VARIANTS } from "@/features/vehicles/lib/vehicle-status";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";

function Rows({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="divide-y divide-slate-100">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between gap-4 py-2 text-sm">
          <dt className="text-slate-500">{label}</dt>
          <dd className="text-right font-medium text-slate-900">{value || "-"}</dd>
        </div>
      ))}
    </dl>
  );
}

function VehicleDetailContent({ id }: { id: string }) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const vehicle = useAsync(() => vehiclesService.get(id), [id]);
  const driver = useAsync(() => (can(PERMISSIONS.DRIVERS_READ) ? vehiclesService.currentDriver(id) : Promise.resolve(undefined)), [id]);
  const [pending, setPending] = useState<(typeof STATUS_ACTIONS)[number] | null>(null);

  if (vehicle.error) return <EmptyState title="Véhicule introuvable" description={getErrorMessage(vehicle.error, "Il a peut-être été supprimé.")} />;
  if (!vehicle.data) return <Skeleton className="mx-auto h-96 max-w-4xl" />;
  const v = vehicle.data;
  const reload = () => { vehicle.reload(); driver.reload(); };

  const makeAvailable = async () => {
    try {
      await vehiclesService.changeStatus(id, "AVAILABLE");
      addToast({ title: "Succès", description: "Véhicule remis disponible.", type: "success" });
      reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de changer le statut."), type: "error" });
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <BackLink href="/vehicles" label="Véhicules" />
      <PageHeader
        title={`${v.brand} ${v.model} · ${v.fleetNumber}`}
        description={v.plate}
        action={(
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={STATUS_VARIANTS[v.status]}>{STATUS_LABELS[v.status]}</Badge>
            {can(PERMISSIONS.VEHICLES_UPDATE) && v.status !== "ASSIGNED" && (
              <Dropdown trigger={<Button variant="outline">Changer le statut</Button>}>
                {STATUS_ACTIONS.filter((action) => action.status !== v.status).map((action) => (
                  <DropdownItem key={action.status} onClick={() => (action.status === "AVAILABLE" ? makeAvailable() : setPending(action))}>{action.label}</DropdownItem>
                ))}
              </Dropdown>
            )}
          </div>
        )}
      />
      {v.statusReason && v.status !== "AVAILABLE" && v.status !== "ASSIGNED" && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-900">Motif : {v.statusReason}</p>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardTitle>Véhicule</CardTitle>
          <Rows rows={[
            ["Numéro", v.fleetNumber],
            ["Marque et modèle", `${v.brand} ${v.model}`],
            ["Année", String(v.year)],
            ["Immatriculation", v.plate],
            ["Couleur", <ColorLabel key="c" value={v.color} />],
            ["N° de série (VIN)", v.vin],
            ["Prix d'achat", formatFcfa(v.purchasePrice)],
            ["Loyer journalier", formatFcfa(v.dailyRent)],
          ]} />
        </Card>
        <Card>
          <CardTitle>Chauffeur</CardTitle>
          {!can(PERMISSIONS.DRIVERS_READ) ? (
            <p className="text-sm text-slate-500">Vous n&apos;avez pas accès aux chauffeurs.</p>
          ) : driver.isLoading && !driver.data ? <Skeleton className="h-16 w-full" /> : driver.data ? (
            <Rows rows={[
              ["Nom", <Link key="n" href={`/drivers/${driver.data.id}`} className="text-primary hover:underline">{driver.data.personal.lastName} {driver.data.personal.firstName}</Link>],
              ["Téléphone", driver.data.personal.phonePrimary],
              ["Email", driver.data.personal.email],
            ]} />
          ) : (
            <p className="text-sm text-slate-500">Ce véhicule n&apos;est loué à aucun chauffeur.</p>
          )}
        </Card>
      </div>

      {pending && (
        <ReasonDialog title={pending.title} description={`${v.fleetNumber} · ${v.plate}`} confirmText="Confirmer" onClose={() => setPending(null)}
          onConfirm={async (reason) => {
            await vehiclesService.changeStatus(id, pending.status, reason);
            setPending(null);
            addToast({ title: "Succès", description: "Statut mis à jour.", type: "success" });
            reload();
          }} />
      )}
    </div>
  );
}

export function VehicleDetail({ id }: { id: string }) {
  return (
    <RequirePermission permission={PERMISSIONS.VEHICLES_READ}>
      <VehicleDetailContent id={id} />
    </RequirePermission>
  );
}
