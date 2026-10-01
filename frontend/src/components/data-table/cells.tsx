import { formatFcfa } from "@/lib/money";
import { cn } from "@/lib/utils";

export type Tone = "good" | "watch" | "alert" | "bad" | "neutral" | "info";

const DOT: Record<Tone, string> = { good: "bg-emerald-600", watch: "bg-amber-500", alert: "bg-orange-600", bad: "bg-red-600", neutral: "bg-slate-400", info: "bg-sky-600" };

/** A status as a coloured dot and a word: dense, readable, never a wall of pills. */
export function Status({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  return <span className="inline-flex items-center gap-2 whitespace-nowrap"><span aria-hidden className={cn("h-2 w-2 rounded-full", DOT[tone])} />{children}</span>;
}

/** Two lines: the thing, then a quieter detail. */
export function Stack({ primary, secondary }: { primary: React.ReactNode; secondary?: React.ReactNode }) {
  return (
    <div className="min-w-0 max-w-[26rem]">
      <div className="truncate font-medium text-ink">{primary}</div>
      {secondary ? <div className="truncate text-xs text-muted">{secondary}</div> : null}
    </div>
  );
}

export function Money({ value, muted }: { value: number; muted?: boolean }) {
  return <span className={cn("tabular-nums", muted && "text-muted")}>{formatFcfa(value)}</span>;
}

export function None() {
  return <span className="text-muted">-</span>;
}
