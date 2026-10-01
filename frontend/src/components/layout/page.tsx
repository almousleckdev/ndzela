import Link from "next/link";
import { cn } from "@/lib/utils";

/** The content column: wide enough for tables, never edge to edge on large screens. */
export function Page({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={cn("mx-auto w-full max-w-[1360px] space-y-6", className)}>{children}</div>;
}

interface PageHeadProps {
  title: string;
  description?: string;
  /** Primary and secondary actions, right-aligned. */
  actions?: React.ReactNode;
}

export function PageHead({ title, description, actions }: PageHeadProps) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
      <div className="min-w-0">
        <h1 className="font-display text-2xl font-semibold tracking-tight text-ink">{title}</h1>
        {description && <p className="mt-1 max-w-2xl text-sm text-muted">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

/** A flat, bordered surface: the only container the console uses. */
export function Panel({ children, className }: { children: React.ReactNode; className?: string }) {
  return <section className={cn("rounded-lg border border-line bg-white", className)}>{children}</section>;
}

export function PanelHead({ title, hint, action }: { title: string; hint?: string; action?: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-line px-4 py-3">
      <div className="min-w-0">
        <h2 className="text-sm font-semibold text-ink">{title}</h2>
        {hint && <p className="text-xs text-muted">{hint}</p>}
      </div>
      {action}
    </div>
  );
}

export interface Kpi {
  label: string;
  value: React.ReactNode;
  hint?: React.ReactNode;
  href?: string;
  /** "bad" colours the figure red: something needs attention. */
  tone?: "bad";
}

/** The key figures of a page as one band with hairline dividers, not a row of look-alike cards. */
export function KpiBand({ items, className }: { items: Kpi[]; className?: string }) {
  return (
    <div className={cn("overflow-hidden rounded-lg border border-line bg-white", className)}>
      <dl className={cn("-mb-px -mr-px grid grid-cols-2 sm:grid-cols-3", items.length >= 4 && "lg:grid-cols-4", items.length >= 5 && items.length !== 7 && "xl:grid-cols-5", items.length === 6 && "xl:grid-cols-6")}>
        {items.map((item) => {
          const body = (
            <div className={cn("h-full border-b border-r border-line px-4 py-3.5", item.href && "transition-colors hover:bg-slate-50")}>
              <dt className="text-[13px] text-muted">{item.label}</dt>
              <dd className={cn("mt-1 font-display text-[1.65rem] font-semibold leading-none tabular-nums tracking-tight", item.tone === "bad" ? "text-red-700" : "text-ink")}>{item.value}</dd>
              {item.hint && <p className="mt-1.5 text-xs text-muted">{item.hint}</p>}
            </div>
          );
          return item.href ? <Link key={item.label} href={item.href} className="block focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary">{body}</Link> : <div key={item.label}>{body}</div>;
        })}
      </dl>
    </div>
  );
}

/** Table toolbar: search and filters on the left, actions on the right. */
export function Toolbar({ start, end }: { start: React.ReactNode; end?: React.ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 p-4">
      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">{start}</div>
      {end && <div className="flex flex-wrap items-center gap-2">{end}</div>}
    </div>
  );
}
