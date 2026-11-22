"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { getErrorMessage } from "@/lib/errors";
import { DriverPicker } from "@/features/free-charging/components/driver-picker";
import { agreementSchema, CONDITIONS_MAX, type AgreementValues } from "@/features/free-charging/schemas/agreement";
import type { Agreement, AgreementInput } from "@/features/free-charging/types";

interface AgreementFormDialogProps {
  agreement?: Agreement;
  windowText?: string;
  onClose: () => void;
  onSubmit: (driverId: string, input: AgreementInput) => Promise<void>;
}

export function AgreementFormDialog({ agreement, windowText = "pendant le créneau gratuit", onClose, onSubmit }: AgreementFormDialogProps) {
  const [failure, setFailure] = useState<string | null>(null);
  const { register, control, handleSubmit, formState } = useForm<AgreementValues>({
    resolver: zodResolver(agreementSchema),
    defaultValues: { driverId: agreement?.driverId ?? "", startsOn: agreement?.startsOn ?? "", endsOn: agreement?.endsOn ?? "", conditions: agreement?.conditions ?? "" },
  });
  const { errors } = formState;

  const submit = async ({ driverId, startsOn, endsOn, conditions }: AgreementValues) => {
    setFailure(null);
    try {
      await onSubmit(driverId, { startsOn, endsOn: endsOn || null, conditions: conditions.trim() || null });
    } catch (error) {
      setFailure(getErrorMessage(error, "Impossible d'enregistrer l'accord."));
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={agreement ? "Modifier l'accord" : "Ajouter un chauffeur"}
      description={agreement ? `${agreement.driverName}. Les dates et les conditions de la gratuité.` : `Un chauffeur avec un accord actif recharge gratuitement une fois par jour de gratuité, ${windowText}, s'il n'a aucun jour impayé.`}>
      <form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        {!agreement && <Controller control={control} name="driverId" render={({ field }) => <DriverPicker value={field.value} onChange={field.onChange} error={errors.driverId?.message} />} />}
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Date de début *" type="date" error={errors.startsOn?.message} {...register("startsOn")} />
          <FormField label="Date de fin" type="date" hint="Laissez vide pour un accord sans fin." error={errors.endsOn?.message} {...register("endsOn")} />
        </div>
        <div>
          <Label htmlFor="agreement-conditions">Conditions</Label>
          <textarea id="agreement-conditions" rows={3} maxLength={CONDITIONS_MAX + 100} aria-invalid={errors.conditions ? true : undefined}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" {...register("conditions")} />
          {errors.conditions && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{errors.conditions.message}</p>}
        </div>
        {failure && <p role="alert" className="text-sm font-medium text-red-600">{failure}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={formState.isSubmitting}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer</Button>
        </div>
      </form>
    </Dialog>
  );
}
