"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { useDebounced } from "@/hooks/use-debounced";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { depositSchema } from "@/features/contracts/schemas";
import { contractsService } from "@/features/contracts/services/contracts.service";

const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR", { day: "numeric", month: "long", year: "numeric" });

/** The contract's financial terms for this car and deposit, computed by the server (rent from the car, start today, 1 248 billable days). */
export function ContractRecap({ vehicleId, deposit }: { vehicleId: string; deposit: string }) {
  const debounced = useDebounced(deposit);
  const valid = Boolean(vehicleId) && depositSchema.safeParse(debounced).success;
  const terms = useAsync(() => (valid ? contractsService.preview(vehicleId, Number(debounced)) : Promise.resolve(null)), [valid, vehicleId, debounced]);

  if (!vehicleId) return <p className="text-sm text-slate-500">Choisissez d&apos;abord un véhicule à l&apos;étape précédente.</p>;
  if (!valid) return <p className="text-sm text-slate-500">Saisissez la caution pour voir le récapitulatif.</p>;
  if (terms.error) return <p role="alert" className="text-sm font-medium text-red-600">{getErrorMessage(terms.error, "Impossible de calculer le récapitulatif.")}</p>;
  if (!terms.data) return <Skeleton className="h-40 w-full" />;

  const t = terms.data;
  const rows: [string, string][] = [
    ["Date de début", day(t.startDate)],
    ["Durée", `${t.billableDays.toLocaleString("fr-FR")} jours facturables (lundi au samedi)`],
    ["Date de fin prévue", day(t.endDate)],
    ["Loyer journalier", formatFcfa(t.dailyRent)],
    ["Prix total du véhicule", formatFcfa(t.totalPrice)],
    ["Caution versée", formatFcfa(t.deposit)],
  ];
  return (
    <dl className="divide-y divide-slate-100 rounded-lg border border-slate-200">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between gap-4 px-4 py-2.5 text-sm">
          <dt className="text-slate-500">{label}</dt>
          <dd className="text-right font-medium text-slate-900">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
