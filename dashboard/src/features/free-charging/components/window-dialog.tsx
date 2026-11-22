"use client";

import { useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { getErrorMessage } from "@/lib/errors";
import { WEEKDAY_OPTIONS } from "@/features/free-charging/lib/free-charging-window";
import { windowSchema, type WindowValues } from "@/features/free-charging/schemas/window";
import type { FreeWindow, FreeWindowInput } from "@/features/free-charging/types";

interface WindowDialogProps {
  window: FreeWindow;
  onClose: () => void;
  onSubmit: (input: FreeWindowInput) => Promise<void>;
}

export function WindowDialog({ window, onClose, onSubmit }: WindowDialogProps) {
  const [failure, setFailure] = useState<string | null>(null);
  const { register, control, handleSubmit, formState } = useForm<WindowValues>({
    resolver: zodResolver(windowSchema),
    defaultValues: { day: window.day, from: window.from, until: window.endOfDay ? "" : window.until, endOfDay: window.endOfDay },
  });
  const { errors } = formState;
  const endOfDay = useWatch({ control, name: "endOfDay" });

  const submit = async ({ day, from, until, endOfDay: toTheEnd }: WindowValues) => {
    setFailure(null);
    try {
      await onSubmit({ day, from, until: toTheEnd ? null : until, endOfDay: toTheEnd });
    } catch (error) {
      setFailure(getErrorMessage(error, "Impossible d'enregistrer le créneau."));
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Modifier le créneau gratuit"
      description="Le jour et les heures pendant lesquels un chauffeur peut recharger gratuitement. Le changement vaut pour les prochaines recharges ; une recharge déjà en cours garde sa fin.">
      <form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        <SelectField label="Jour *" options={WEEKDAY_OPTIONS.map(({ value, label }) => ({ value, label }))} error={errors.day?.message} {...register("day")} />
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Début *" type="time" error={errors.from?.message} {...register("from")} />
          <FormField label="Fin" type="time" disabled={endOfDay} error={errors.until?.message} {...register("until")} />
        </div>
        <Controller control={control} name="endOfDay" render={({ field }) => (
          <Checkbox label="Jusqu'à la fin de la journée (23h59)" checked={field.value} onChange={(event) => field.onChange(event.target.checked)} />
        )} />
        {failure && <p role="alert" className="text-sm font-medium text-red-600">{failure}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={formState.isSubmitting}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer</Button>
        </div>
      </form>
    </Dialog>
  );
}
