"use client";

import { useState } from "react";
import Link from "next/link";
import { format } from "date-fns";
import { fr } from "date-fns/locale";
import { BatteryCharging, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Page, PageHead, Panel, PanelHead, KpiBand, type Kpi } from "@/components/layout/page";
import { None, Money } from "@/components/data-table/cells";
import { useAsync } from "@/hooks/use-async";
import { formatDateTime } from "@/lib/time";
import { cn } from "@/lib/utils";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { chargingService } from "@/features/charging/services/charging.service";
import { DistributionBar } from "@/features/dashboard/components/distribution-bar";
import { driversService } from "@/features/drivers/services/drivers.service";
import { RecordPaymentDialog } from "@/features/payments/components/record-payment-dialog";
import { LEVEL_LABELS, METHOD_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { AccountRow } from "@/features/payments/types";
import { STATUS_LABELS } from "@/features/vehicles/lib/vehicle-status";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";
import { formatFcfa } from "@/lib/money";

const LEVEL_MARK = { NONE: "bg-transparent", LATE: "bg-slate-400", YELLOW: "bg-yellow-400", DARK_YELLOW: "bg-amber-500", RED: "bg-red-600" } as const;
const QUEUE_SIZE = 6;
const FEED_SIZE = 5;
const NO_QUERY = { type: "PAYMENT", method: "ALL", from: "", to: "" } as const;
const localToday = () => format(new Date(), "yyyy-MM-dd");

export function DashboardPage() {
  const { can } = usePermissions();
  const mayPayments = can(PERMISSIONS.PAYMENTS_READ);
  const mayCharging = can(PERMISSIONS.CHARGING_READ);
  const mayDrivers = can(PERMISSIONS.DRIVERS_READ);
  const mayVehicles = can(PERMISSIONS.VEHICLES_READ);

  const payments = useAsync(() => (mayPayments ? paymentsService.summary() : Promise.resolve(null)), [mayPayments]);
  const queue = useAsync(() => (mayPayments ? paymentsService.accounts("LATE", "", 0, QUEUE_SIZE, "overdueSince,asc") : Promise.resolve(null)), [mayPayments]);
  const feed = useAsync(() => (mayPayments ? paymentsService.history({ ...NO_QUERY }, 0, FEED_SIZE) : Promise.resolve(null)), [mayPayments]);
  const charging = useAsync(() => (mayCharging ? chargingService.summary() : Promise.resolve(null)), [mayCharging]);
  const recharges = useAsync(() => (mayCharging ? chargingService.recharges({ from: localToday(), to: localToday() }, 0, FEED_SIZE) : Promise.resolve(null)), [mayCharging]);
  const drivers = useAsync(() => (mayDrivers ? driversService.stats() : Promise.resolve(null)), [mayDrivers]);
  const vehicles = useAsync(() => (mayVehicles ? vehiclesService.counts() : Promise.resolve(null)), [mayVehicles]);
  const [paying, setPaying] = useState<AccountRow | null>(null);
  const reloadMoney = () => { payments.reload(); queue.reload(); feed.reload(); };

  const kpis: Kpi[] = [];
  if (mayPayments) {
    const p = payments.data;
    kpis.push({ label: "Encaissé aujourd'hui", value: p ? formatFcfa(p.collectedToday) : "-", hint: "Loyers reçus depuis minuit", href: "/finance/payments" });
    kpis.push({ label: "Loyers impayés", value: p ? formatFcfa(p.totalOverdue) : "-", hint: p ? `${p.late} chauffeur(s) en retard` : undefined, href: "/finance/arrears", tone: p && p.totalOverdue > 0 ? "bad" : undefined });
  }
  if (mayCharging) {
    const c = charging.data;
    kpis.push({ label: "Recharges du jour", value: c ? formatFcfa(c.fcfaToday) : "-", hint: c ? `${c.rechargesToday} recharge(s), ${c.criticalAccounts} arrivée(s) critique(s)` : undefined, href: "/charging" });
  }
  if (mayVehicles) {
    const v = vehicles.data;
    const total = v ? Object.values(v).reduce((sum, value) => sum + value, 0) : 0;
    kpis.push({ label: "Véhicules loués", value: v ? `${v.ASSIGNED} / ${total}` : "-", hint: v ? `${v.AVAILABLE} disponible(s)` : undefined, href: "/vehicles" });
  }

  return (
    <Page>
      <PageHead
        title="Tableau de bord"
        description={format(new Date(), "EEEE d MMMM yyyy", { locale: fr })}
        actions={(
          <>
            {can(PERMISSIONS.CHARGING_CREATE) && <Link href="/charging"><Button variant="outline"><BatteryCharging className="mr-2 h-4 w-4" /> Enregistrer une recharge</Button></Link>}
            {can(PERMISSIONS.PAYMENTS_CREATE) && <Link href="/finance/payments"><Button><Wallet className="mr-2 h-4 w-4" /> Enregistrer un paiement</Button></Link>}
          </>
        )}
      />

      {kpis.length > 0 ? <KpiBand items={kpis} /> : (
        <Panel className="p-6 text-sm text-muted">Votre rôle ne donne accès à aucun indicateur. Utilisez le menu pour ouvrir les pages qui vous sont ouvertes.</Panel>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          {mayPayments && (
            <Panel>
              <PanelHead title="À traiter maintenant" hint="Les loyers impayés, du plus ancien au plus récent" action={<Link href="/finance/arrears" className="text-sm font-medium text-primary-ink hover:underline">Voir tous les impayés</Link>} />
              {!queue.data ? (
                <div className="space-y-2 p-4"><Skeleton className="h-10 w-full" /><Skeleton className="h-10 w-full" /><Skeleton className="h-10 w-full" /></div>
              ) : queue.data.items.length === 0 ? (
                <p className="px-4 py-10 text-center text-sm text-muted">Aucun impayé. Tous les chauffeurs sont à jour.</p>
              ) : (
                <ul className="divide-y divide-line/70">
                  {queue.data.items.map((row) => (
                    <li key={row.driverId} className="relative flex flex-wrap items-center gap-x-4 gap-y-1 px-4 py-3">
                      <span aria-hidden className={cn("absolute inset-y-0 left-0 w-[3px]", LEVEL_MARK[row.level])} />
                      <div className="min-w-0 flex-1">
                        <Link href={`/drivers/${row.driverId}`} className="truncate font-medium text-ink hover:text-primary-ink">{row.driverName}</Link>
                        <p className="truncate text-xs text-muted">{row.vehicle ?? "Sans véhicule"} · {LEVEL_LABELS[row.level]}{row.overdueSince ? ` depuis le ${formatDateTime(row.overdueSince)}` : ""}</p>
                      </div>
                      <div className="text-right font-display text-lg font-semibold tabular-nums text-ink">{formatFcfa(row.overdueAmount)}</div>
                      {can(PERMISSIONS.PAYMENTS_CREATE) && row.contractId && <Button size="sm" variant="outline" onClick={() => setPaying(row)}>Encaisser</Button>}
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          )}

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {mayPayments && (
              <Panel>
                <PanelHead title="Derniers encaissements" action={<Link href="/finance/history" className="text-sm font-medium text-primary-ink hover:underline">Historique</Link>} />
                {!feed.data ? <div className="p-4"><Skeleton className="h-24 w-full" /></div> : feed.data.items.length === 0 ? <p className="px-4 py-8 text-center text-sm text-muted">Aucun paiement enregistré.</p> : (
                  <ul className="divide-y divide-line/70">
                    {feed.data.items.map((entry) => (
                      <li key={entry.id} className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm">
                        <div className="min-w-0"><p className="truncate font-medium text-ink">{entry.driverName ?? <None />}</p><p className="truncate text-xs text-muted">{entry.method ? METHOD_LABELS[entry.method] : ""} · {formatDateTime(entry.createdAt)}</p></div>
                        <Money value={entry.amount} muted={entry.reversed} />
                      </li>
                    ))}
                  </ul>
                )}
              </Panel>
            )}
            {mayCharging && (
              <Panel>
                <PanelHead title="Recharges d'aujourd'hui" action={<Link href="/charging" className="text-sm font-medium text-primary-ink hover:underline">Toutes</Link>} />
                {!recharges.data ? <div className="p-4"><Skeleton className="h-24 w-full" /></div> : recharges.data.items.length === 0 ? <p className="px-4 py-8 text-center text-sm text-muted">Aucune recharge aujourd&apos;hui.</p> : (
                  <ul className="divide-y divide-line/70">
                    {recharges.data.items.map((r) => (
                      <li key={r.id} className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm">
                        <div className="min-w-0"><p className="truncate font-medium text-ink">{r.vehicle}</p><p className={cn("truncate text-xs", r.critical ? "font-medium text-red-700" : "text-muted")}>{r.batteryBefore} % → {r.batteryAfter} % · {r.station}</p></div>
                        <Money value={r.amount} muted={r.reversed} />
                      </li>
                    ))}
                  </ul>
                )}
              </Panel>
            )}
          </div>
        </div>

        <div className="space-y-6">
          {mayVehicles && (
            <Panel>
              <PanelHead title="Flotte" action={<Link href="/vehicles" className="text-sm font-medium text-primary-ink hover:underline">Véhicules</Link>} />
              <div className="p-4">
                {!vehicles.data ? <Skeleton className="h-28 w-full" /> : (
                  <DistributionBar empty="Aucun véhicule dans la flotte." segments={[
                    { label: STATUS_LABELS.ASSIGNED, value: vehicles.data.ASSIGNED, color: "bg-sidebar" },
                    { label: STATUS_LABELS.AVAILABLE, value: vehicles.data.AVAILABLE, color: "bg-emerald-600" },
                    { label: STATUS_LABELS.IN_MAINTENANCE, value: vehicles.data.IN_MAINTENANCE, color: "bg-amber-500" },
                    { label: STATUS_LABELS.ACCIDENT, value: vehicles.data.ACCIDENT, color: "bg-red-600" },
                    { label: STATUS_LABELS.SUSPENDED, value: vehicles.data.SUSPENDED, color: "bg-orange-500" },
                    { label: STATUS_LABELS.OUT_OF_SERVICE, value: vehicles.data.OUT_OF_SERVICE, color: "bg-slate-400" },
                  ]} />
                )}
              </div>
            </Panel>
          )}
          {mayDrivers && (
            <Panel>
              <PanelHead title="Chauffeurs" action={<Link href="/drivers" className="text-sm font-medium text-primary-ink hover:underline">Chauffeurs</Link>} />
              <div className="p-4">
                {!drivers.data ? <Skeleton className="h-28 w-full" /> : (
                  <DistributionBar empty="Aucun chauffeur enregistré." segments={[
                    { label: "Actifs", value: drivers.data.active, color: "bg-emerald-600" },
                    { label: "En arrêt", value: drivers.data.onLeave, color: "bg-sky-600" },
                    { label: "Inactifs", value: drivers.data.inactive, color: "bg-slate-400" },
                    { label: "Suspendus", value: drivers.data.suspended, color: "bg-amber-500" },
                    { label: "Bloqués", value: drivers.data.blocked, color: "bg-red-600" },
                  ]} />
                )}
                {drivers.data && <p className="mt-3 border-t border-line pt-3 text-xs text-muted">{drivers.data.assigned} avec un véhicule, {drivers.data.withoutVehicle} sans véhicule.</p>}
              </div>
            </Panel>
          )}
        </div>
      </div>

      {paying && <RecordPaymentDialog driverId={paying.driverId} driverName={paying.driverName} overdueAmount={paying.overdueAmount} dailyRent={paying.dailyRent} onClose={() => setPaying(null)} onSaved={() => { setPaying(null); reloadMoney(); }} />}
    </Page>
  );
}
