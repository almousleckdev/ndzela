"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { cn } from "@/lib/utils";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { RecordPaymentDialog } from "@/features/payments/components/record-payment-dialog";
import { WaiverDialog } from "@/features/payments/components/waiver-dialog";
import { DAY_STATUS_CLASS, DAY_STATUS_LABELS, ENTRY_TYPE_LABELS, LEVEL_BADGE, LEVEL_LABELS, METHOD_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";

const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR", { weekday: "short", day: "numeric", month: "short" });

// The driver's payment status
export function DriverPaymentsCard({ driverId, onChanged }: { driverId: string; onChanged: () => void }) {
  const { can } = usePermissions();
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  const [paying, setPaying] = useState(false);
  const [waiving, setWaiving] = useState(false);
  const a = account.data;
  const refresh = () => { account.reload(); onChanged(); };

  return (
    <Card>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <CardTitle>Paiements</CardTitle>
        {a?.contractId && (
          <div className="flex flex-wrap gap-2">
            {can(PERMISSIONS.PAYMENTS_CREATE) && <Button onClick={() => setPaying(true)}>Enregistrer un paiement</Button>}
            {can(PERMISSIONS.ARREARS_MANAGE) && a.overdueAmount > 0 && <Button variant="outline" onClick={() => setWaiving(true)}>Accorder un délai</Button>}
          </div>
        )}
      </div>
      {!a ? <Skeleton className="h-32 w-full" /> : !a.contractId ? (
        <p className="text-sm text-slate-500">Pas de contrat actif : aucun loyer n&apos;est dû.</p>
      ) : (
        <div className="space-y-5">
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div><p className="text-xs text-slate-500">Niveau</p><span className={cn("mt-1 inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium", LEVEL_BADGE[a.level])}>{LEVEL_LABELS[a.level]}</span></div>
            <div><p className="text-xs text-slate-500">Montant dû</p><p className="text-lg font-semibold text-slate-900">{formatFcfa(a.overdueAmount)}</p></div>
            <div><p className="text-xs text-slate-500">Crédit</p><p className="text-lg font-semibold text-slate-900">{formatFcfa(a.credit)}</p></div>
            <div><p className="text-xs text-slate-500">À temps / en retard / impayés</p><p className="text-lg font-semibold text-slate-900">{a.paidOnTime} / {a.paidLate} / {a.unpaid}</p></div>
          </div>
          {a.overdueSince && <p className="text-xs text-slate-500">En retard depuis le {formatDateTime(a.overdueSince)}.</p>}
          <p className={cn("text-xs", a.weekSettled ? "text-emerald-700" : "text-amber-700")}>{a.weekSettled ? "Semaine réglée" : "Semaine non réglée"} · clôture le {formatDateTime(a.weekClosesAt)}</p>

          <div>
            <h3 className="mb-2 text-sm font-semibold text-slate-900">Derniers jours</h3>
            {a.days.length === 0 ? <p className="text-sm text-slate-500">Aucun loyer échu pour l&apos;instant.</p> : (
              <ul className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                {a.days.slice(0, 14).map((d) => (
                  <li key={d.day} className="flex items-center justify-between rounded-md border border-slate-100 px-3 py-1.5 text-sm">
                    <span>{day(d.day)}{d.settledAt && <span className="ml-2 text-xs text-slate-500">payé le {formatDateTime(d.settledAt)}</span>}</span>
                    <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", DAY_STATUS_CLASS[d.status])}>{DAY_STATUS_LABELS[d.status]}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-900">Dernières écritures</h3>
              <Link href={`/finance/history`} className="text-xs text-primary hover:underline">Tout l&apos;historique</Link>
            </div>
            {a.entries.length === 0 ? <p className="text-sm text-slate-500">Aucune écriture.</p> : (
              <ul className="divide-y divide-slate-100 text-sm">
                {a.entries.slice(0, 6).map((e) => (
                  <li key={e.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                    <span>{ENTRY_TYPE_LABELS[e.type]}{e.method ? ` · ${METHOD_LABELS[e.method]}` : ""}{e.reversed ? " (annulé)" : ""}<span className="ml-2 text-xs text-slate-500">{formatDateTime(e.createdAt)}</span></span>
                    <span className={cn("font-medium", e.reversed ? "text-slate-400 line-through" : "text-slate-900")}>{formatFcfa(e.amount)}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
      {paying && a && <RecordPaymentDialog driverId={driverId} driverName={a.driverName} overdueAmount={a.overdueAmount} dailyRent={a.dailyRent} onClose={() => setPaying(false)} onSaved={() => { setPaying(false); refresh(); }} />}
      {waiving && a && <WaiverDialog driverId={driverId} driverName={a.driverName} overdueAmount={a.overdueAmount} dailyRent={a.dailyRent} onClose={() => setWaiving(false)} onSaved={() => { setWaiving(false); refresh(); }} />}
    </Card>
  );
}
