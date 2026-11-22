"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { getErrorMessage } from "@/lib/errors";
import { AgreementTermsFields } from "@/features/free-charging/components/agreement-terms-fields";
import { termsSchema, type TermsValues } from "@/features/free-charging/schemas/terms";
import type { Agreement, AgreementInput } from "@/features/free-charging/types";

interface AgreementEditDialogProps {
  agreement: Agreement;
  onClose: () => void;
  onSubmit: (input: AgreementInput) => Promise<void>;
}

export function AgreementEditDialog({ agreement, onClose, onSubmit }: AgreementEditDialogProps) {
  const [failure, setFailure] = useState<string | null>(null);
  const { register, handleSubmit, formState } = useForm<TermsValues>({
    resolver: zodResolver(termsSchema),
    defaultValues: { startsOn: agreement.startsOn, endsOn: agreement.endsOn ?? "", conditions: agreement.conditions ?? "" },
  });

  const submit = async ({ startsOn, endsOn, conditions }: TermsValues) => {
    setFailure(null);
    try {
      await onSubmit({ startsOn, endsOn: endsOn || null, conditions: conditions.trim() || null });
    } catch (error) {
      setFailure(getErrorMessage(error, "Impossible d'enregistrer l'accord."));
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Modifier l'accord" description={agreement.driverName}>
      <form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        <AgreementTermsFields register={register} errors={formState.errors} />
        {failure && <p role="alert" className="text-sm font-medium text-red-600">{failure}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={formState.isSubmitting}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer</Button>
        </div>
      </form>
    </Dialog>
  );
}
