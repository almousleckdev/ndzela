"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { AgreementTermsFields } from "@/features/free-charging/components/agreement-terms-fields";
import { BulkOutcome } from "@/features/free-charging/components/bulk-outcome";
import { DriverMultiPicker } from "@/features/free-charging/components/driver-multi-picker";
import { useCreateMany } from "@/features/free-charging/hooks/use-create-many";
import { termsSchema, type TermsValues } from "@/features/free-charging/schemas/terms";
import type { DriverOption } from "@/features/free-charging/types";

interface AgreementCreateDialogProps {
  windowText?: string;
  onClose: () => void;
  onDone: (count: number) => void;
}

export function AgreementCreateDialog({ windowText = "pendant le créneau gratuit", onClose, onDone }: AgreementCreateDialogProps) {
  const many = useCreateMany();
  const [selected, setSelected] = useState<DriverOption[]>([]);
  const [missing, setMissing] = useState(false);
  const { register, handleSubmit, formState } = useForm<TermsValues>({ resolver: zodResolver(termsSchema), defaultValues: { startsOn: "", endsOn: "", conditions: "" } });
  const finished = !many.running && (many.added > 0 || many.failures.length > 0);
  const label = selected.length > 1 ? `Ajouter ${selected.length} chauffeurs` : "Ajouter";

  const submit = async ({ startsOn, endsOn, conditions }: TermsValues) => {
    setMissing(false);
    const failed = await many.run(selected, { startsOn, endsOn: endsOn || null, conditions: conditions.trim() || null });
    if (failed.length === 0) onDone(selected.length);
    else setSelected(failed.map((failure) => failure.driver));
  };

  const validate = (values: TermsValues) => {
    if (selected.length === 0) {
      setMissing(true);
      return Promise.resolve();
    }
    return submit(values);
  };

  return (
    <Dialog isOpen onClose={onClose} title="Ajouter un chauffeur"
      description={`Un chauffeur avec un accord actif recharge gratuitement une fois par jour de gratuité, ${windowText}, s'il n'a aucun jour impayé. Vous pouvez en choisir plusieurs.`}>
      <form onSubmit={handleSubmit(validate, () => setMissing(selected.length === 0))} className="space-y-4" noValidate>
        {finished && <BulkOutcome added={many.added} failures={many.failures} />}
        <DriverMultiPicker selected={selected} onChange={setSelected} disabled={many.running} error={missing ? "Choisissez au moins un chauffeur." : undefined} />
        <AgreementTermsFields register={register} errors={formState.errors} />
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={many.running}>{finished ? "Terminé" : "Annuler"}</Button>
          <Button type="submit" isLoading={many.running}>{many.progress ? `Ajout ${many.progress.done} / ${many.progress.total}…` : finished ? "Réessayer" : label}</Button>
        </div>
      </form>
    </Dialog>
  );
}
