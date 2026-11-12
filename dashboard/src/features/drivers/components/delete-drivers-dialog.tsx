"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

// Deletes one or several drivers. Only a holder of `drivers:purge` can also erase the money recorded on them, after ticking the box.
export function DeleteDriversDialog({ drivers, onClose, onDone }: { drivers: Driver[]; onClose: () => void; onDone: () => void }) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const mayPurge = can(PERMISSIONS.DRIVERS_PURGE);
  const mustPurge = mayPurge && !can(PERMISSIONS.DRIVERS_DELETE);
  const [withMoney, setWithMoney] = useState(mustPurge);
  const [sure, setSure] = useState(false);
  const [isRunning, setIsRunning] = useState(false);
  const names = drivers.map((driver) => `${driver.personal.lastName} ${driver.personal.firstName}`);
  const one = drivers.length === 1;

  const run = async () => {
    setIsRunning(true);
    const failed: string[] = [];
    let lastError: unknown;
    for (const [index, driver] of drivers.entries()) {
      try {
        if (withMoney) await driversService.purge(driver.id); else await driversService.remove(driver.id);
      } catch (error) {
        failed.push(names[index]);
        lastError = error;
      }
    }
    const done = drivers.length - failed.length;
    if (done > 0) addToast({ title: "Succès", description: done === 1 ? "Chauffeur supprimé." : `${done} chauffeurs supprimés.`, type: "success" });
    if (failed.length > 0) addToast({ title: "Erreur", description: `${failed.join(", ")} : ${getErrorMessage(lastError, "suppression impossible.")}`, type: "error" });
    setIsRunning(false);
    if (done > 0) onDone(); else onClose();
  };

  return (
    <Dialog isOpen onClose={onClose} title={one ? "Supprimer ce chauffeur ?" : `Supprimer ${drivers.length} chauffeurs ?`}
      description={one ? names[0] : names.slice(0, 5).join(", ") + (names.length > 5 ? `… (+${names.length - 5})` : "")}>
      <div className="space-y-3">
        <p className="text-sm text-slate-600">
          {one ? "Le chauffeur est supprimé" : "Les chauffeurs sont supprimés"} avec leurs contrats, leurs arrêts et leurs affectations ; les véhicules redeviennent disponibles. Les loyers impayés sont abandonnés.
          {!withMoney && " Impossible si un paiement ou une recharge est enregistré."}
        </p>
        {mayPurge && !mustPurge && (
          <Checkbox label="Supprimer aussi l'argent enregistré" description="Paiements, abandons de loyer et recharges du chauffeur sont effacés définitivement."
            checked={withMoney} onChange={(event) => { setWithMoney(event.target.checked); setSure(false); }} />
        )}
        {withMoney && (
          <>
            <p role="alert" className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
              Les paiements, abandons de loyer et recharges {one ? "de ce chauffeur" : "de ces chauffeurs"} seront effacés définitivement. Seul un résumé reste dans l&apos;historique.
            </p>
            <Checkbox label="Je suis sûr de vouloir tout supprimer" checked={sure} onChange={(event) => setSure(event.target.checked)} />
          </>
        )}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isRunning}>Annuler</Button>
          <Button variant="destructive" onClick={run} isLoading={isRunning} disabled={withMoney && !sure}>Supprimer</Button>
        </div>
      </div>
    </Dialog>
  );
}
