"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { BackLink } from "@/components/layout/back-link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { DriverActionDialogs, availableActions, type DriverAction } from "@/features/drivers/components/driver-actions";
import { DriverAvatar } from "@/features/drivers/components/driver-avatar";
import { DriverLeavesDialog } from "@/features/drivers/components/driver-leaves-dialog";
import { DriverPaymentsCard } from "@/features/payments/components/driver-payments-card";
import { DriverFilesCard } from "@/features/drivers/components/driver-files-card";
import { AssignVehicleDialog } from "@/features/drivers/components/assign-vehicle-dialog";
import { CIVILITY_LABELS, displayStatus, ID_TYPE_LABELS, RELATIONSHIP_LABELS } from "@/features/drivers/lib/driver-labels";
import { driversService } from "@/features/drivers/services/drivers.service";

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

const date = (iso: string) => new Date(iso).toLocaleDateString("fr-FR");

function DriverDetailContent({ id }: { id: string }) {
  const router = useRouter();
  const { can } = usePermissions();
  const driver = useAsync(() => driversService.get(id), [id]);
  const [action, setAction] = useState<DriverAction | null>(null);
  const [leaves, setLeaves] = useState(false);
  const [photoVersion, setPhotoVersion] = useState(0);
  const [assigning, setAssigning] = useState(false);
  const isGlobal = useAuthStore((state) => state.user?.country === null);
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const mustPickCountry = isGlobal && selectedCountry === null;

  if (driver.error) return <EmptyState title="Chauffeur introuvable" description={getErrorMessage(driver.error, "Il a peut-être été supprimé.")} />;
  if (!driver.data) return <Skeleton className="mx-auto h-96 max-w-4xl" />;
  const d = driver.data;

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <BackLink href="/drivers" label="Chauffeurs" />
      {d.statusReason && d.status !== "ACTIVE" && (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-900">Motif : {d.statusReason}</p>
      )}
      <div className="flex items-center gap-4">
        <DriverAvatar driver={d} size="xl" version={photoVersion} />
      </div>
      <PageHeader
        title={`${d.personal.lastName} ${d.personal.firstName}`}
        description={`${d.personal.email} · ${d.personal.phonePrimary}`}
        action={(
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={displayStatus(d).variant}>{displayStatus(d).label}</Badge>
            <Button variant="outline" onClick={() => setLeaves(true)}>Gérer les arrêts</Button>
            {can(PERMISSIONS.DRIVERS_UPDATE) && <Link href={`/drivers/${id}/edit`}><Button variant="outline">Modifier</Button></Link>}
            {availableActions(d, can).filter((item) => item.kind !== "remove-vehicle").map((item) => (
              <Button key={item.kind} variant="outline" className={item.destructive ? "text-red-600 hover:bg-red-50" : undefined} onClick={() => setAction({ kind: item.kind, driver: d })}>{item.label}</Button>
            ))}
          </div>
        )}
      />

      <Card>
        <CardTitle>Véhicule</CardTitle>
        {d.vehicle ? (
          <>
            <Rows rows={[
              ["Numéro", d.vehicle.fleetNumber],
              ["Véhicule", `${d.vehicle.brand} ${d.vehicle.model} (${d.vehicle.year})`],
              ["Immatriculation", d.vehicle.plate],
              ["Loyer journalier", formatFcfa(d.vehicle.dailyRent)],
            ]} />
            {can(PERMISSIONS.DRIVERS_UPDATE) && <div className="mt-4"><Button variant="outline" onClick={() => setAction({ kind: "remove-vehicle", driver: d })}>Retirer le véhicule</Button></div>}
          </>
        ) : (
          <div className="flex items-center justify-between gap-4">
            <p className="text-sm text-slate-500">Ce chauffeur n&apos;a pas de véhicule.</p>
            {can(PERMISSIONS.DRIVERS_UPDATE) && (
              <div className="text-right">
                <Button onClick={() => setAssigning(true)} disabled={mustPickCountry}>Affecter un véhicule</Button>
                {mustPickCountry && <p className="mt-1 text-xs text-slate-500">Choisissez un pays dans l&apos;en-tête.</p>}
              </div>
            )}
          </div>
        )}
      </Card>

      <Card>
        <CardTitle>Contrat</CardTitle>
        {d.contract ? (
          <>
            <Rows rows={[
              ["Numéro", d.contract.contractNumber],
              ["Début", date(d.contract.startDate)],
              ["Fin prévue", date(d.contract.endDate)],
              ["Loyer journalier", formatFcfa(d.contract.dailyRent)],
              ["Caution", formatFcfa(d.contract.deposit)],
              ["Prix total du véhicule", formatFcfa(d.contract.totalPrice)],
              ["Révision", d.contract.revision === 0 ? "Contrat d'origine" : `Mis à jour ${d.contract.revision} fois`],
            ]} />
            {can(PERMISSIONS.CONTRACTS_READ) && <div className="mt-4"><Link href={`/contracts/${d.contract.id}`}><Button variant="outline">Voir et imprimer le contrat</Button></Link></div>}
          </>
        ) : (
          <p className="text-sm text-slate-500">Aucun contrat actif : le contrat est créé quand un véhicule est affecté.</p>
        )}
      </Card>

      {can(PERMISSIONS.PAYMENTS_READ) && <DriverPaymentsCard driverId={id} onChanged={() => driver.reload()} />}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardTitle>Informations personnelles</CardTitle>
          <Rows rows={[
            ["Civilité", CIVILITY_LABELS[d.personal.civility]],
            ["Naissance", `${date(d.personal.birthDate)} à ${d.personal.birthPlace}`],
            ["Nationalité", d.personal.nationality],
            [ID_TYPE_LABELS[d.personal.idType], d.personal.idNumber],
            ["Délivrée", `${date(d.personal.idIssueDate)} à ${d.personal.idIssuePlace}`],
            ["Adresse", `${d.personal.address}, ${d.personal.district}, ${d.personal.city}`],
            ["Téléphone secondaire", d.personal.phoneSecondary],
            ["Permis", `${d.personal.licenceNumber} (catégorie ${d.personal.licenceCategory})`],
            ["Permis délivré le", date(d.personal.licenceIssueDate)],
          ]} />
        </Card>
        <div className="space-y-6">
          <Card>
            <CardTitle>Contact en cas d&apos;urgence</CardTitle>
            <Rows rows={[
              ["Nom", `${d.emergency.lastName} ${d.emergency.firstName}`],
              ["Téléphone", d.emergency.phone],
              ["Lien de parenté", RELATIONSHIP_LABELS[d.emergency.relationship]],
              ["Adresse", d.emergency.address],
            ]} />
          </Card>
          <Card>
            <CardTitle>Formation</CardTitle>
            <Rows rows={[
              ["Centre", d.training.trainingCenter.name],
              ["Date de formation", date(d.training.trainingDate)],
              ["N° de certificat", d.training.certificateNumber],
            ]} />
          </Card>
        </div>
      </div>

      <DriverFilesCard driver={d} canEdit={can(PERMISSIONS.DRIVERS_UPDATE)} onChanged={() => { setPhotoVersion((v) => v + 1); driver.reload(); }} />

      {leaves && <DriverLeavesDialog driver={d} canDeclare={can(PERMISSIONS.DRIVERS_UPDATE)} onClose={() => setLeaves(false)} onChanged={() => driver.reload()} />}

      {assigning && <AssignVehicleDialog driver={d} onClose={() => setAssigning(false)} onSaved={() => { setAssigning(false); driver.reload(); }} />}
      <DriverActionDialogs target={action} onClose={() => setAction(null)}
        onDone={(kind) => { setAction(null); if (kind === "delete") router.replace("/drivers"); else driver.reload(); }} />
    </div>
  );
}

export function DriverDetail({ id }: { id: string }) {
  return (
    <RequirePermission permission={PERMISSIONS.DRIVERS_READ}>
      <DriverDetailContent id={id} />
    </RequirePermission>
  );
}
