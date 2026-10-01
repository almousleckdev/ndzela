"use client";

import type { UseFormReturn } from "react-hook-form";
import { Checkbox } from "@/components/ui/checkbox";
import { FormField } from "@/components/ui/form-field";
import { formatFcfa } from "@/lib/money";
import { ContractRecap } from "@/features/contracts/components/contract-recap";
import type { Contract } from "@/features/contracts/types";
import type { DriverFormValues } from "@/features/drivers/schemas/driver";

interface ContractStepProps {
  form: UseFormReturn<DriverFormValues>;
  /** Present when editing a driver who has an active contract: the step then offers to update it. */
  existing?: Contract;
  updateExisting?: boolean;
  onToggleUpdate?: (value: boolean) => void;
}

const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR", { day: "numeric", month: "long", year: "numeric" });

/** Step 4. New driver: the deposit and the financial recap. Existing driver: the contract in force, its deposit, and whether to refresh it with the new details. */
export function ContractStep({ form, existing, updateExisting, onToggleUpdate }: ContractStepProps) {
  const { register, formState, watch } = form;

  if (existing) {
    const rows: [string, string][] = [
      ["Numéro", existing.contractNumber],
      ["Début", day(existing.startDate)],
      ["Fin prévue", day(existing.endDate)],
      ["Loyer journalier", formatFcfa(existing.dailyRent)],
      ["Prix total du véhicule", formatFcfa(existing.totalPrice)],
      ["Révision", existing.revision === 0 ? "Contrat d'origine" : `Mis à jour ${existing.revision} fois`],
    ];
    return (
      <div className="space-y-6">
        <dl className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {rows.map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 px-4 py-2.5 text-sm"><dt className="text-slate-500">{label}</dt><dd className="text-right font-medium text-slate-900">{value}</dd></div>
          ))}
        </dl>
        <Checkbox label="Mettre à jour le contrat avec ces informations" description="Les informations du chauffeur et du véhicule imprimées dans le contrat sont remplacées par les valeurs actuelles. Le numéro, les dates et le loyer ne changent pas."
          checked={Boolean(updateExisting)} onChange={(event) => onToggleUpdate?.(event.target.checked)} />
        {updateExisting && (
          <FormField label="Caution (FCFA) *" inputMode="numeric" hint="Modifiez-la seulement si elle a changé." error={formState.errors.contract?.deposit?.message} {...register("contract.deposit")} />
        )}
        <p className="text-xs text-slate-500">Après l&apos;enregistrement, le contrat mis à jour s&apos;affiche pour impression.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <FormField label="Caution (FCFA) *" inputMode="numeric" hint="Dépôt de garantie versé par le chauffeur. Le loyer journalier est celui du véhicule et ne peut pas être modifié."
        error={formState.errors.contract?.deposit?.message} {...register("contract.deposit")} />
      <div>
        <h3 className="mb-2 text-sm font-semibold text-slate-900">Récapitulatif financier</h3>
        <ContractRecap vehicleId={watch("vehicleId")} deposit={watch("contract.deposit")} />
      </div>
      <p className="text-xs text-slate-500">Après la création du compte, le contrat en français s&apos;affiche pour impression.</p>
    </div>
  );
}
