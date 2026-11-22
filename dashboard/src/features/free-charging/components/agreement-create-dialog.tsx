"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { AgreementTermsFields } from "@/features/free-charging/components/agreement-terms-fields";
import { BulkOutcome } from "@/features/free-charging/components/bulk-outcome";
import { DriverPicker } from "@/features/free-charging/components/driver-picker";
import { useApplyChanges } from "@/features/free-charging/hooks/use-apply-changes";
import { NO_SELECTION, pendingCounts, type Selection } from "@/features/free-charging/lib/selection";
import { termsSchema, type TermsValues } from "@/features/free-charging/schemas/terms";

interface AgreementCreateDialogProps {
  windowText?: string;
  onClose: () => void;
  onDone: (added: number, removed: number) => void;
}

export function AgreementCreateDialog({ windowText = "pendant le créneau gratuit", onClose, onDone }: AgreementCreateDialogProps) {
  const apply = useApplyChanges();
  const [selection, setSelection] = useState<Selection>(NO_SELECTION);
  const [empty, setEmpty] = useState(false);
  const counts = pendingCounts(selection);
  const { register, handleSubmit, formState } = useForm<TermsValues>({ resolver: zodResolver(termsSchema), defaultValues: { startsOn: "", endsOn: "", conditions: "" } });
  const finished = !apply.running && apply.result !== null;

  const save = async ({ startsOn, endsOn, conditions }: TermsValues) => {
    const failures = await apply.run(Object.values(selection.adds), Object.values(selection.removes), { startsOn, endsOn: endsOn || null, conditions: conditions.trim() || null });
    if (failures.length === 0) return onDone(counts.add, counts.remove);
    const left = new Set(failures.map((failure) => failure.driver.driverId));
    const keep = (side: Selection["adds"]) => Object.fromEntries(Object.entries(side).filter(([id]) => left.has(id)));
    setSelection({ adds: keep(selection.adds), removes: keep(selection.removes) });
  };

  const submit = (values: TermsValues) => {
    setEmpty(counts.add + counts.remove === 0);
    return counts.add + counts.remove === 0 ? Promise.resolve() : save(values);
  };

  const removalsOnly = counts.add === 0 && counts.remove > 0;
  const submitRemovals = () => save({ startsOn: "", endsOn: "", conditions: "" });

  return (
    <Dialog isOpen onClose={onClose} title="Ajouter un chauffeur" className="max-w-5xl"
      description={`Tous les chauffeurs. Cochez un chauffeur pour lui offrir la recharge gratuite ${windowText}, décochez un chauffeur du programme pour le retirer. Un chauffeur sans jour impayé recharge gratuitement une fois par jour de gratuité.`}>
      <form onSubmit={removalsOnly ? (event) => { event.preventDefault(); void submitRemovals(); } : handleSubmit(submit, () => setEmpty(counts.add + counts.remove === 0))} className="space-y-4" noValidate>
        {apply.result && finished && <BulkOutcome result={apply.result} />}
        <DriverPicker selection={selection} onChange={setSelection} onClear={() => setSelection(NO_SELECTION)} disabled={apply.running} />
        {empty && <p role="alert" className="text-xs font-medium text-red-600">Cochez ou décochez au moins un chauffeur.</p>}
        {counts.add > 0 && <AgreementTermsFields register={register} errors={formState.errors} />}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={apply.running}>{finished ? "Terminé" : "Annuler"}</Button>
          <Button type="submit" isLoading={apply.running}>{apply.progress ? `Enregistrement ${apply.progress.done} / ${apply.progress.total}…` : finished ? "Réessayer" : "Enregistrer les changements"}</Button>
        </div>
      </form>
    </Dialog>
  );
}
