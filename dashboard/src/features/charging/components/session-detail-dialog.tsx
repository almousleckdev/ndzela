"use client";

import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { batteryLabel, durationLabel, PAYMENT_STATE, reasonLabel, statusLabel } from "@/features/charging/components/session-format";
import type { ChargingSessionRow } from "@/features/charging/types";

function Rows({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="divide-y divide-slate-100">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between gap-4 py-1.5 text-sm"><dt className="text-slate-500">{label}</dt><dd className="text-right font-medium text-slate-900">{value ?? "-"}</dd></div>
      ))}
    </dl>
  );
}

const money = (value: number | null) => (value === null ? null : formatFcfa(value));

// One session with who charged and the breakdown of what the driver paid, for disputes.
export function SessionDetailDialog({ session: s, onClose }: { session: ChargingSessionRow; onClose: () => void }) {
  const p = s.payment;
  const state = p?.state ? PAYMENT_STATE[p.state] : undefined;
  const battery = batteryLabel(s);
  return (
    <Dialog isOpen onClose={onClose} title={`Session ${s.transactionId}`} description={s.chargePointId ? `${s.chargePointId}${s.connectorId ? ` · connecteur ${s.connectorId}` : ""}` : undefined} className="max-w-xl">
      <div className="space-y-5">
        <section>
          <h3 className="mb-1 text-sm font-semibold text-slate-900">Qui a rechargé</h3>
          <Rows rows={[
            ["Chauffeur", s.holder ? <Link key="d" href={`/drivers/${s.holder.driverId}`} className="text-primary hover:underline">{s.holder.driverName}</Link> : "Public"],
            ["Véhicule", s.holder?.vehicle && s.holder.vehicleId ? <Link key="v" href={`/vehicles/${s.holder.vehicleId}`} className="text-primary hover:underline">{s.holder.vehicle}</Link> : null],
            ["Identifiant", s.idTag],
          ]} />
        </section>
        <section>
          <h3 className="mb-1 text-sm font-semibold text-slate-900">La recharge</h3>
          <Rows rows={[
            ["Début", s.startedAt ? formatDateTime(s.startedAt) : null],
            ["Fin", s.endedAt ? formatDateTime(s.endedAt) : null],
            ["Durée", durationLabel(s)],
            ["Énergie", s.energyKwh === null ? null : `${s.energyKwh} kWh`],
            ["Batterie", battery?.primary],
            ["Statut", statusLabel(s.status)],
            ["Motif d'arrêt", reasonLabel(s.stopReason)],
            ["Montant calculé (énergie × tarif)", money(s.amount)],
          ]} />
        </section>
        <section>
          <h3 className="mb-1 flex items-center gap-2 text-sm font-semibold text-slate-900">Ce que le chauffeur a payé {state && <Badge variant={state.variant}>{state.label}</Badge>}</h3>
          {p ? (
            <>
              <Rows rows={[
                ["Énergie", money(p.energyCost)],
                ["Frais de service", money(p.serviceFee)],
                ["TVA", money(p.vat)],
                ["Total débité du portefeuille", money(p.total)],
                ["Moyen de paiement", p.method === "WALLET" ? "Portefeuille Zella" : p.method],
              ]} />
              {p.breakdownEstimated && <p className="mt-2 text-xs text-slate-500">Le détail est estimé : cette recharge a été réglée avant que le détail ne soit enregistré.</p>}
              {p.capped && <p className="mt-2 text-xs text-amber-700">La recharge a été limitée au budget autorisé : le total débité est inférieur à la somme des lignes.</p>}
            </>
          ) : <p className="text-sm text-slate-500">Aucun paiement reçu de Zella pour cette recharge (client public, compte non lié ou paiement pas encore remonté).</p>}
        </section>
        <div className="flex justify-end border-t border-slate-200 pt-4"><Button variant="outline" onClick={onClose}>Fermer</Button></div>
      </div>
    </Dialog>
  );
}
