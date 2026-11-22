import type { FieldErrors, UseFormRegister } from "react-hook-form";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { CONDITIONS_MAX, type TermsValues } from "@/features/free-charging/schemas/terms";

interface AgreementTermsFieldsProps {
  register: UseFormRegister<TermsValues>;
  errors: FieldErrors<TermsValues>;
}

export function AgreementTermsFields({ register, errors }: AgreementTermsFieldsProps) {
  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Début" type="date" error={errors.startsOn?.message} {...register("startsOn")} />
        <FormField label="Fin (facultatif)" type="date" hint="Sans date de fin, l'accord reste actif jusqu'à ce que vous le suspendiez ou le retiriez." error={errors.endsOn?.message} {...register("endsOn")} />
      </div>
      <div>
        <Label htmlFor="agreement-conditions">Conditions (facultatif)</Label>
        <textarea id="agreement-conditions" rows={3} maxLength={CONDITIONS_MAX + 100} aria-invalid={errors.conditions ? true : undefined}
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" {...register("conditions")} />
        {errors.conditions && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{errors.conditions.message}</p>}
      </div>
    </>
  );
}
