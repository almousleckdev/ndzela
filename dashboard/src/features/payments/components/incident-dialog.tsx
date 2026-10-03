"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { SelectField } from "@/components/ui/select-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { IncidentCause, IncidentKind } from "@/features/payments/types";

interface IncidentDialogProps {
  driverId: string;
  driverName: string;
  kind: IncidentKind;
  onClose: () => void;
  onSaved: () => void;
}

const COPY: Record<IncidentKind, { title: string; hint: string; confirm: string; done: string }> = {
  HALF_DAY: { title: "Déclarer une demi-journée", hint: "Coupure d'électricité de 4 à 8 h : le chauffeur ne paie que la moitié du loyer de ce jour.", confirm: "Déclarer la demi-journée", done: "Demi-journée déclarée." },
  STOP: { title: "Déclarer un arrêt", hint: "Coupure de plus de 8 h : le chauffeur ne paie pas ce jour. Les dimanches sont déjà exclus.", confirm: "Déclarer l'arrêt", done: "Arrêt déclaré." },
};

function yesterday(): string {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

// An administrator declares that a past day was half or not payable (electricity cut or another cause)
export function IncidentDialog({ driverId, driverName, kind, onClose, onSaved }: IncidentDialogProps) {
  const { addToast } = useToast();
  const copy = COPY[kind];
  const [day, setDay] = useState(yesterday());
  const [cause, setCause] = useState<IncidentCause>("ELECTRICITY");
  const [hours, setHours] = useState(kind === "HALF_DAY" ? "4" : "9");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const change = <T,>(set: (value: T) => void) => (value: T) => { set(value); setError(undefined); };

  const save = async () => {
    if (!day) return setError("Choisissez le jour concerné.");
    if (cause === "ELECTRICITY" && !/^\d{1,2}$/.test(hours)) return setError("Saisissez la durée de la coupure en heures.");
    if (cause === "OTHER" && !note.trim()) return setError("Précisez la cause dans la note.");
    setIsSaving(true);
    try {
      await paymentsService.declareIncident(driverId, { day, kind, cause, hours: cause === "ELECTRICITY" ? Number(hours) : undefined, note: note.trim() || undefined });
      addToast({ title: "Succès", description: copy.done, type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible de déclarer cet événement."));
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={copy.title} description={`${driverName}. ${copy.hint}`}>
      <div className="space-y-4">
        <FormField label="Jour concerné *" type="date" max={yesterday()} value={day} onChange={(event) => change(setDay)(event.target.value)} hint="Un jour passé du contrat." />
        <SelectField label="Cause *" options={[{ value: "ELECTRICITY", label: "Coupure d'électricité" }, { value: "OTHER", label: "Autre" }]} value={cause} onChange={(event) => change(setCause)(event.target.value as IncidentCause)} />
        {cause === "ELECTRICITY" && <FormField label="Durée de la coupure (heures) *" inputMode="numeric" value={hours} onChange={(event) => change(setHours)(event.target.value)} />}
        <div>
          <Label htmlFor="incident-note">{cause === "OTHER" ? "Note *" : "Note"}</Label>
          <textarea id="incident-note" rows={2} maxLength={255} value={note} onChange={(event) => change(setNote)(event.target.value)}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
          <Button onClick={save} isLoading={isSaving}>{copy.confirm}</Button>
        </div>
      </div>
    </Dialog>
  );
}
