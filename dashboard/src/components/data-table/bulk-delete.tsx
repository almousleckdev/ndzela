"use client";

import { useState } from "react";
import { BulkBar } from "@/components/data-table/bulk-bar";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";

interface BulkDeleteProps<T> {
  // The rows of the page on screen: a selection left on another page is never deleted by mistake.
  items: T[];
  selected: ReadonlySet<string>;
  onSelectedChange: (next: Set<string>) => void;
  idOf: (item: T) => string;
  nameOf: (item: T) => string;
  // Deletes one row; the reason is "" unless `withReason`.
  remove: (item: T, reason: string) => Promise<unknown>;
  noun: { one: string; many: string };
  withReason?: boolean;
  note?: string;
  onDone: () => void;
}

// "N sélectionnés" strip with a delete button, and the confirmation. Rows are deleted one by one; failures are listed.
export function BulkDelete<T>({ items, selected, onSelectedChange, idOf, nameOf, remove, noun, withReason, note, onDone }: BulkDeleteProps<T>) {
  const { addToast } = useToast();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [isRunning, setIsRunning] = useState(false);
  const chosen = items.filter((item) => selected.has(idOf(item)));
  const names = chosen.map(nameOf);

  const run = async () => {
    setIsRunning(true);
    const failed: string[] = [];
    let lastError: unknown;
    for (const item of chosen) {
      try { await remove(item, reason.trim()); } catch (error) { failed.push(nameOf(item)); lastError = error; }
    }
    const done = chosen.length - failed.length;
    if (done > 0) addToast({ title: "Succès", description: done === 1 ? `1 ${noun.one} supprimé.` : `${done} ${noun.many} supprimés.`, type: "success" });
    if (failed.length > 0) addToast({ title: "Erreur", description: `${failed.join(", ")} : ${getErrorMessage(lastError, "suppression impossible.")}`, type: "error" });
    setIsRunning(false);
    setOpen(false);
    setReason("");
    onSelectedChange(new Set());
    if (done > 0) onDone();
  };

  return (
    <>
      <BulkBar count={chosen.length} onClear={() => onSelectedChange(new Set())}>
        <Button variant="destructive" onClick={() => setOpen(true)}>Supprimer la sélection</Button>
      </BulkBar>
      {open && (
        <Dialog isOpen onClose={() => !isRunning && setOpen(false)} title={chosen.length === 1 ? `Supprimer ce ${noun.one} ?` : `Supprimer ${chosen.length} ${noun.many} ?`}
          description={names.slice(0, 5).join(", ") + (names.length > 5 ? `… (+${names.length - 5})` : "")}>
          <div className="space-y-3">
            <p className="text-sm text-slate-600">{note ?? "La suppression est définitive."}</p>
            {withReason && (
              <div>
                <Label htmlFor="bulk-reason">Motif *</Label>
                <textarea id="bulk-reason" rows={2} maxLength={255} value={reason} onChange={(event) => setReason(event.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
              </div>
            )}
            <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
              <Button variant="outline" onClick={() => setOpen(false)} disabled={isRunning}>Annuler</Button>
              <Button variant="destructive" onClick={run} isLoading={isRunning} disabled={withReason && !reason.trim()}>Supprimer</Button>
            </div>
          </div>
        </Dialog>
      )}
    </>
  );
}
