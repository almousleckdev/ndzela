import Link from "next/link";
import { format } from "date-fns";
import { Skeleton } from "@/components/ui/skeleton";
import { Panel, PanelHead } from "@/components/layout/page";
import { Money, None } from "@/components/data-table/cells";
import type { ChargingSessionRow } from "@/features/charging/types";

// The latest network sessions of today (Zella), not the recharges staff record by hand.
export function TodaySessionsPanel({ sessions }: { sessions: ChargingSessionRow[] | null }) {
  return (
    <Panel>
      <PanelHead title="Recharges d'aujourd'hui" action={<Link href="/charging" className="text-sm font-medium text-primary-ink hover:underline">Toutes</Link>} />
      {!sessions ? <div className="p-4"><Skeleton className="h-24 w-full" /></div> : sessions.length === 0 ? <p className="px-4 py-8 text-center text-sm text-muted">Aucune recharge aujourd&apos;hui.</p> : (
        <ul className="divide-y divide-line/70">
          {sessions.map((s) => (
            <li key={s.id} className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm">
              <div className="min-w-0">
                <p className="truncate font-medium text-ink">{s.holder?.driverName ?? "Client public"}</p>
                <p className="truncate text-xs text-muted">{s.startedAt ? format(new Date(s.startedAt), "HH:mm") : "-"}{s.energyKwh !== null ? ` · ${s.energyKwh.toLocaleString("fr-FR")} kWh` : ""}{s.holder?.vehicle ? ` · ${s.holder.vehicle}` : ""}</p>
              </div>
              {s.amount === null ? <None /> : <Money value={s.amount} />}
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}
