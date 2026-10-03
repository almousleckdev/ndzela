"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { ContractRecap } from "@/features/contracts/components/contract-recap";
import { DEFAULT_DEPOSIT, depositSchema } from "@/features/contracts/schemas";
import { driversService } from "@/features/drivers/services/drivers.service";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";
import type { Driver } from "@/features/drivers/types";

// Gives an available vehicle to a driver who has none.
export function AssignVehicleDialog({ driver, onClose, onSaved }: { driver: Driver; onClose: () => void; onSaved: () => void }) {
  const { addToast } = useToast();
  const vehicles = useFetch(() => vehiclesService.list({ status: "AVAILABLE", q: "", page: 0, size: 100 }));
  const [vehicleId, setVehicleId] = useState("");
  const [deposit, setDeposit] = useState(DEFAULT_DEPOSIT);
  const [isSaving, setIsSaving] = useState(false);
  const items = vehicles.data?.items ?? [];

  const save = async () => {
    if (!vehicleId || !depositSchema.safeParse(deposit).success) return;
    setIsSaving(true);
    try {
      await driversService.assignVehicle(driver.id, vehicleId, Number(deposit));
      addToast({ title: "Succès", description: "Véhicule affecté et contrat créé.", type: "success" });
      onSaved();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'affecter le véhicule."), type: "error" });
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Affecter un véhicule" description={`${driver.personal.lastName} ${driver.personal.firstName}`}>
      <div className="space-y-4">
        <SelectField label="Véhicule disponible" value={vehicleId} onChange={(event) => setVehicleId(event.target.value)}
          placeholder={items.length === 0 ? "Aucun véhicule disponible" : "Choisir un véhicule"}
          options={items.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.fleetNumber} · ${vehicle.brand} ${vehicle.model} · ${vehicle.plate}` }))} />
        <FormField label="Caution (FCFA) *" inputMode="numeric" hint="Un nouveau contrat est créé avec ce véhicule." value={deposit} onChange={(event) => setDeposit(event.target.value)}
          error={depositSchema.safeParse(deposit).success ? undefined : "Montant invalide"} />
        <ContractRecap vehicleId={vehicleId} deposit={deposit} />
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Annuler</Button>
          <Button onClick={save} isLoading={isSaving} disabled={!vehicleId || !depositSchema.safeParse(deposit).success}>Affecter</Button>
        </div>
      </div>
    </Dialog>
  );
}
