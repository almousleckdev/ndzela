import { cn } from "@/lib/utils";

export interface Segment {
  label: string;
  value: number;
  // A Tailwind background class.
  color: string;
}

// One stacked bar and its legend
export function DistributionBar({ segments, empty }: { segments: Segment[]; empty: string }) {
  const total = segments.reduce((sum, segment) => sum + segment.value, 0);
  if (total === 0) return <p className="text-sm text-muted">{empty}</p>;
  return (
    <div>
      <div className="flex h-2.5 w-full overflow-hidden rounded-full bg-slate-100" role="img" aria-label={segments.filter((s) => s.value > 0).map((s) => `${s.label} : ${s.value}`).join(", ")}>
        {segments.filter((segment) => segment.value > 0).map((segment) => <span key={segment.label} className={cn("h-full", segment.color)} style={{ width: `${(segment.value / total) * 100}%` }} />)}
      </div>
      <ul className="mt-3 space-y-1.5 text-sm">
        {segments.map((segment) => (
          <li key={segment.label} className="flex items-center justify-between gap-3">
            <span className="flex items-center gap-2 text-ink"><span aria-hidden className={cn("h-2 w-2 rounded-full", segment.color)} />{segment.label}</span>
            <span className="tabular-nums text-muted"><span className="font-medium text-ink">{segment.value}</span> · {Math.round((segment.value / total) * 100)} %</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
