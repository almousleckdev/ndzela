import { None } from "@/components/data-table/cells";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import type { PaidDay } from "@/features/payments/types";

const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR", { weekday: "short", day: "numeric", month: "short" });

// The rent days a payment settled, and when it was paid
export function PaidDays({ days, paidAt }: { days: PaidDay[]; paidAt: string | null }) {
  if (days.length === 0) return <None />;
  return (
    <div className="space-y-1 text-sm">
      {days.map((d) => (
        <p key={`${d.day}-${d.kind}`} className="flex items-center gap-2 whitespace-nowrap">
          <span className="font-medium">{d.kind === "PENALTY" ? "Pénalité " : ""}{day(d.day)}</span>
          <span className={d.onTime ? "rounded-full bg-emerald-50 px-2 py-0.5 text-xs text-emerald-700" : "rounded-full bg-amber-50 px-2 py-0.5 text-xs text-amber-700"}>{d.onTime ? "à temps" : "en retard"}</span>
          <span className="text-xs text-muted">{formatFcfa(d.amount)}</span>
        </p>
      ))}
      {paidAt && <p className="text-xs text-muted">payé le {formatDateTime(paidAt)}</p>}
    </div>
  );
}
