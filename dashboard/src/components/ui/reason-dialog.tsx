"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";

interface ReasonDialogProps {
  title: string;
  description?: string;
  confirmText: string;
  destructive?: boolean;
  onClose: () => void;
  // Extra controls shown under the reason
  children?: React.ReactNode;
  // Receives the trimmed reason; a thrown error is shown and the dialog stays open.
  onConfirm: (reason: string) => Promise<void>;
}

const MAX_LENGTH = 255;

// Asks for the mandatory reason of a restrictive action (suspend, block, remove...).
export function ReasonDialog({ title, description, confirmText, destructive = false, onClose, onConfirm, children }: ReasonDialogProps) {
  const { addToast } = useToast();
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  const submit = async () => {
    const value = reason.trim();
    if (!value) return setError("Le motif est obligatoire.");
    setIsSaving(true);
    try {
      await onConfirm(value);
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "L'action a échoué."), type: "error" });
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={title} description={description}>
      <div className="space-y-4">
        <div>
          <Label htmlFor="reason">Motif *</Label>
          <textarea
            id="reason"
            rows={3}
            maxLength={MAX_LENGTH}
            value={reason}
            aria-invalid={error ? true : undefined}
            onChange={(event) => { setReason(event.target.value); setError(undefined); }}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
          {error && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
        </div>
        {children}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
          <Button variant={destructive ? "destructive" : "default"} onClick={submit} isLoading={isSaving}>{confirmText}</Button>
        </div>
      </div>
    </Dialog>
  );
}
