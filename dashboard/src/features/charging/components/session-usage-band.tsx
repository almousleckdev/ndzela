"use client";

import { useAsync } from "@/hooks/use-async";
import { usePolling } from "@/hooks/use-polling";
import { formatFcfa } from "@/lib/money";
import { chargingService } from "@/features/charging/services/charging.service";
import type { SessionKind, SessionPeriod, SessionScope } from "@/features/charging/types";

const kwh = (value: number) => `${new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 1 }).format(value)} kWh`;

function PeriodCell({ title, period }: { title: string; period: SessionPeriod | undefined }) {
  const rows: [string, string][] = [
    ["Recharges", period ? String(period.sessions) : "-"],
    ["Énergie", period ? kwh(period.energyKwh) : "-"],
    ["Encaissé", period ? formatFcfa(period.collected) : "-"],
  ];
  return (
    <div className="h-full border-b border-r border-line px-4 py-3.5">
      <h3 className="text-[13px] text-muted">{title}</h3>
      <dl className="mt-2 space-y-1">
        {rows.map(([label, value]) => (
          <div key={label} className="flex items-baseline justify-between gap-2">
            <dt className="text-xs text-muted">{label}</dt>
            <dd className="font-display text-base font-semibold tabular-nums text-ink">{value}</dd>
          </div>
        ))}
      </dl>
      {period && period.staffEnergyKwh > 0 && <p className="mt-1.5 text-xs text-muted">dont {kwh(period.staffEnergyKwh)} du personnel NDZela, non facturé</p>}
    </div>
  );
}

// Charges, energy and money collected today, yesterday, this week and this month, for the whole network, one charger or one driver
export function SessionUsageBand({ kind = "ALL", scope = {} }: { kind?: SessionKind; scope?: SessionScope }) {
  const summary = useAsync(() => chargingService.sessionSummary(kind, scope), [kind, scope.chargePoint, scope.driverId]);
  usePolling(() => summary.reload(), 5000);
  const s = summary.data;

  return (
    <div className="overflow-hidden rounded-lg border border-line bg-white">
      <div className="-mb-px -mr-px grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4">
        <PeriodCell title="Aujourd'hui" period={s?.today} />
        <PeriodCell title="Hier" period={s?.yesterday} />
        <PeriodCell title="Cette semaine" period={s?.week} />
        <PeriodCell title="Ce mois-ci" period={s?.month} />
      </div>
    </div>
  );
}
