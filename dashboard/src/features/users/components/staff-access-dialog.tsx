"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { displayName } from "@/features/profile/lib/user";
import { usersService } from "@/features/users/services/users.service";
import type { UserSummary } from "@/features/users/types";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";

type CarKind = "NONE" | "FLEET" | "OWN";

const FLEET = { status: "ALL", q: "", from: "", to: "", sort: "", page: 0, size: 200 } as const;

// The car of a staff member and whether their charges are free: Zella gives the free access when asked.
export function StaffAccessDialog({ user, onClose, onSaved }: { user: UserSummary; onClose: () => void; onSaved: () => void }) {
  const { addToast } = useToast();
  const current = useFetch(() => usersService.staffAccess(user.id));
  const fleet = useFetch(() => vehiclesService.list(FLEET));
  const [kind, setKind] = useState<CarKind | null>(null);
  const [vehicleId, setVehicleId] = useState<string | null>(null);
  const [plate, setPlate] = useState<string | null>(null);
  const [free, setFree] = useState<boolean | null>(null);
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  const saved = current.data;
  const chosenKind = kind ?? (saved?.vehicleId ? "FLEET" : saved?.plate ? "OWN" : "NONE");
  const chosenVehicle = vehicleId ?? saved?.vehicleId ?? "";
  const chosenPlate = plate ?? saved?.plate ?? "";
  const chosenFree = free ?? saved?.freeCharging ?? false;

  const run = async (request: () => Promise<unknown>, success: string) => {
    setIsSaving(true);
    try {
      await request();
      addToast({ title: "Succès", description: success, type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible d'enregistrer l'accès Zella."));
      setIsSaving(false);
    }
  };

  const save = () => {
    if (chosenKind === "FLEET" && !chosenVehicle) return setError("Choisissez la voiture de la flotte.");
    if (chosenKind === "OWN" && !chosenPlate.trim()) return setError("Saisissez la plaque de la voiture.");
    return run(() => usersService.saveStaffAccess(user.id, {
      vehicleId: chosenKind === "FLEET" ? chosenVehicle : undefined,
      plate: chosenKind === "OWN" ? chosenPlate.trim() : undefined,
      freeCharging: chosenFree,
    }), chosenFree ? "Recharge gratuite accordée." : "Accès Zella enregistré.");
  };

  return (
    <Dialog isOpen onClose={onClose} title="Voiture et recharge gratuite" description={`${displayName(user)} est reconnu dans Zella grâce à son e-mail et son téléphone. Une recharge gratuite est comptée en énergie, jamais en argent.`} className="max-w-lg">
      {current.isLoading ? <Skeleton className="h-40 w-full" /> : (
        <div className="space-y-4">
          <SelectField label="Voiture" value={chosenKind} options={[{ value: "NONE", label: "Aucune" }, { value: "FLEET", label: "Voiture de la flotte" }, { value: "OWN", label: "Voiture personnelle" }]}
            onChange={(event) => { setKind(event.target.value as CarKind); setError(undefined); }} />
          {chosenKind === "FLEET" && (
            <SelectField label="Voiture de la flotte *" placeholder="Choisir une voiture" value={chosenVehicle} onChange={(event) => setVehicleId(event.target.value)}
              options={(fleet.data?.items ?? []).map((car) => ({ value: car.id, label: `${car.fleetNumber} · ${car.brand} ${car.model} (${car.plate})` }))} />
          )}
          {chosenKind === "OWN" && <FormField label="Plaque *" value={chosenPlate} maxLength={20} placeholder="GA-123-AB" onChange={(event) => setPlate(event.target.value)} />}
          <Checkbox label="Recharge gratuite (personnel NDZela)" description="Rien n'est débité ni encaissé. L'énergie consommée reste visible dans Zella." checked={chosenFree} onChange={(event) => setFree(event.target.checked)} />
          {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
          <div className="flex justify-between gap-3 border-t border-slate-200 pt-4">
            {saved?.linked ? <Button variant="outline" onClick={() => run(() => usersService.removeStaffAccess(user.id), "Accès Zella retiré.")} disabled={isSaving}>Retirer l&apos;accès</Button> : <span />}
            <div className="flex gap-3">
              <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
              <Button onClick={save} isLoading={isSaving}>Enregistrer</Button>
            </div>
          </div>
        </div>
      )}
    </Dialog>
  );
}
