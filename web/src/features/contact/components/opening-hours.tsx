import { Clock } from "lucide-react";
import { OpenStatus } from "@/features/contact/components/open-status";
import { siteConfig } from "@/config/site";

export function OpeningHours() {
  return (
    <section aria-labelledby="hours-title" className="rounded-3xl bg-sky p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="hours-title" className="flex items-center gap-3 font-display text-xl font-bold"><Clock className="h-5 w-5 text-action" aria-hidden />Horaires d&apos;ouverture</h2>
        <OpenStatus />
      </div>
      <dl className="mt-6 divide-y divide-ink/10">
        {siteConfig.hours.map((entry) => (
          <div key={entry.label} className="flex justify-between gap-4 py-3"><dt className="font-medium">{entry.label}</dt><dd className="tabular-nums text-muted">{entry.text}</dd></div>
        ))}
      </dl>
      <p className="mt-4 text-sm text-muted">Heure de Libreville.</p>
    </section>
  );
}
