import { ColorLabel } from "@/components/ui/color-picker";
import { formatFcfa } from "@/lib/money";
import type { VehicleValues } from "@/features/vehicles/schemas/vehicle";

/** The Récapitulatif shown before a vehicle is saved. */
export function VehicleRecap({ values }: { values: VehicleValues }) {
  const rows: [string, React.ReactNode][] = [
    ["Numéro du véhicule", values.fleetNumber],
    ["Marque", values.brand],
    ["Modèle", values.model],
    ["Année", values.year],
    ["Immatriculation", values.plate.toUpperCase()],
    ["Couleur", <ColorLabel key="color" value={values.color} />],
    ["N° de série (VIN)", values.vin.trim() || "Non renseigné"],
    ["Prix d'achat", formatFcfa(Number(values.purchasePrice))],
    ["Loyer journalier", formatFcfa(Number(values.dailyRent))],
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
