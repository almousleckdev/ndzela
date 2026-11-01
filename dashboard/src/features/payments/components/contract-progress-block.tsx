import { formatFcfa } from "@/lib/money";
import type { ContractProgress } from "@/features/payments/types";

const days = (n: number) => new Intl.NumberFormat("fr-FR").format(n).replace(/ /g, " ");

// The contract since day one: rent days elapsed and paid, money received and still to receive.
export function ContractProgressBlock({ progress: p }: { progress: ContractProgress }) {
  const percent = p.totalDays === 0 ? 0 : Math.min(100, Math.round((p.elapsedDays / p.totalDays) * 100));
  const stats = [
    { label: "Jours payés", value: `${days(p.paidDays)} / ${days(p.elapsedDays)}` },
    { label: "Jours impayés", value: days(p.unpaidDays) },
    { label: "Total payé", value: formatFcfa(p.paidAmount) },
    { label: "Reste à régler", value: formatFcfa(p.remainingAmount) },
  ];
  return (
    <div className="rounded-lg border border-slate-100 bg-slate-50/60 p-4">
      <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-sm font-semibold text-slate-900">Suivi du contrat</h3>
        <p className="text-xs text-slate-500">Jour {days(p.elapsedDays)} sur {days(p.totalDays)} · {days(p.daysLeft)} restants · caution {formatFcfa(p.deposit)}</p>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-slate-200" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100} aria-label="Avancement du contrat">
        <div className="h-full rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
      <dl className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {stats.map((s) => (
          <div key={s.label}><dt className="text-xs text-slate-500">{s.label}</dt><dd className="text-sm font-semibold text-slate-900">{s.value}</dd></div>
        ))}
      </dl>
    </div>
  );
}
