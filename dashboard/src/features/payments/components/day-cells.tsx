import { None } from "@/components/data-table/cells";

// Number of days a driver did not pay, red when there are some.
export function UnpaidDays({ days }: { days: number }) {
  return <span className={days > 0 ? "font-semibold text-red-700" : "text-muted"}>{days}</span>;
}

// Days left in the contract; nothing when the driver has no contract.
export function DaysLeft({ days, hasContract = true }: { days: number; hasContract?: boolean }) {
  return hasContract ? <span className="tabular-nums">{days}</span> : <None />;
}

// Progress through the contract
export function ContractProgress({ elapsed, total }: { elapsed: number; total: number }) {
  if (total <= 0) return <None />;
  const percent = Math.min(100, Math.round((elapsed / total) * 100));
  return (
    <div className="min-w-[8rem]">
      <div className="h-1.5 overflow-hidden rounded-full bg-slate-200" role="progressbar" aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100} aria-label="Progression du contrat">
        <div className="h-full rounded-full bg-primary" style={{ width: `${percent}%` }} />
      </div>
      <p className="mt-1 text-xs tabular-nums text-muted">{elapsed} / {total} jours</p>
    </div>
  );
}
