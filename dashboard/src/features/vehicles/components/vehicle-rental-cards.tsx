import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Card, CardTitle } from "@/components/ui/card";
import { formatFcfa } from "@/lib/money";
import { DetailRows } from "@/features/vehicles/components/detail-rows";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { DRIVER_STATUS_LABELS, DRIVER_STATUS_VARIANTS } from "@/features/drivers/lib/driver-labels";
import { LEVEL_BADGE, LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import type { Driver } from "@/features/drivers/types";

const date = (iso: string) => new Date(iso).toLocaleDateString("fr-FR");

const CONTRACT_STATUS: Record<string, string> = { ACTIVE: "En cours", TERMINATED: "Résilié", COMPLETED: "Terminé" };

// Who drives the car now, and how they stand with their rent.
export function VehicleDriverCard({ driver }: { driver: Driver | null | undefined }) {
  const { can } = usePermissions();
  if (!driver) return <Card><CardTitle>Chauffeur</CardTitle><p className="text-sm text-slate-500">Ce véhicule n&apos;est loué à aucun chauffeur.</p></Card>;
  const { personal: p, arrears: a } = driver;
  return (
    <Card>
      <CardTitle>Chauffeur</CardTitle>
      <DetailRows rows={[
        ["Nom", <Link key="n" href={`/drivers/${driver.id}`} className="text-primary hover:underline">{p.lastName} {p.firstName}</Link>],
        ["Statut", <Badge key="s" variant={DRIVER_STATUS_VARIANTS[driver.status]}>{DRIVER_STATUS_LABELS[driver.status]}</Badge>],
        ["Téléphone", p.phonePrimary],
        ["Email", p.email],
        ["Ville", p.city],
        ...(can(PERMISSIONS.PAYMENTS_READ) ? [
          ["Paiements", <span key="l" className={`rounded-full px-2 py-0.5 text-xs font-medium ${LEVEL_BADGE[a.level]}`}>{LEVEL_LABELS[a.level]}</span>] as [string, React.ReactNode],
          ["Jours impayés", String(a.unpaidDays)] as [string, React.ReactNode],
          ["Montant dû", formatFcfa(a.overdueAmount)] as [string, React.ReactNode],
        ] : []),
      ]} />
    </Card>
  );
}

// The rental: contract terms and progress.
export function VehicleRentalCard({ driver, rent }: { driver: Driver | null | undefined; rent: number }) {
  const { can } = usePermissions();
  const c = driver?.contract;
  if (!driver || !c) return <Card><CardTitle>Location</CardTitle><p className="text-sm text-slate-500">Aucune location en cours. Loyer journalier prévu : {formatFcfa(rent)}.</p></Card>;
  return (
    <Card>
      <CardTitle>Location</CardTitle>
      <DetailRows rows={[
        ["Contrat", can(PERMISSIONS.CONTRACTS_READ) ? <Link key="c" href={`/contracts/${c.id}`} className="text-primary hover:underline">{c.contractNumber}</Link> : c.contractNumber],
        ["Statut du contrat", CONTRACT_STATUS[c.status] ?? c.status],
        ["Début", date(c.startDate)],
        ["Fin prévue", date(c.endDate)],
        ["Jours payés", `${driver.arrears.paidDays} sur ${c.billableDays}`],
        ["Jours restants", String(driver.arrears.daysLeft)],
        ["Loyer journalier", formatFcfa(c.dailyRent)],
        ["Caution", formatFcfa(c.deposit)],
        ["Prix total", formatFcfa(c.totalPrice)],
      ]} />
    </Card>
  );
}
