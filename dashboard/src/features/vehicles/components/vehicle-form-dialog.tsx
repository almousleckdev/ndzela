"use client";

import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { ColorPicker } from "@/components/ui/color-picker";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { vehicleSchema, type VehicleValues } from "@/features/vehicles/schemas/vehicle";
import { vehiclesService } from "@/features/vehicles/services/vehicles.service";
import { VehicleImagesField } from "@/features/vehicles/components/vehicle-images-field";
import { VehicleRecap } from "@/features/vehicles/components/vehicle-recap";
import type { Vehicle } from "@/features/vehicles/types";

interface VehicleFormDialogProps {
  // null = add a vehicle (form, then the recap), otherwise edit that vehicle.
  vehicle: Vehicle | null;
  onClose: () => void;
  onSaved: () => void;
}

export function VehicleFormDialog({ vehicle, onClose, onSaved }: VehicleFormDialogProps) {
  const { addToast } = useToast();
  const [step, setStep] = useState<"form" | "recap">("form");
  const [pendingImages, setPendingImages] = useState<File[]>([]);
  const { register, control, handleSubmit, getValues, formState } = useForm<VehicleValues>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: {
      fleetNumber: vehicle?.fleetNumber ?? "",
      brand: vehicle?.brand ?? "",
      model: vehicle?.model ?? "",
      year: vehicle ? String(vehicle.year) : "",
      plate: vehicle?.plate ?? "",
      color: vehicle?.color ?? "",
      vin: vehicle?.vin ?? "",
      purchasePrice: vehicle ? String(vehicle.purchasePrice) : "",
      dailyRent: vehicle ? String(vehicle.dailyRent) : "",
    },
  });
  const errors = formState.errors;

  const save = async (values: VehicleValues) => {
    try {
      if (vehicle) {
        await vehiclesService.update(vehicle.id, values);
      } else {
        const created = await vehiclesService.create(values);
        if (pendingImages.length > 0) {
          try {
            await vehiclesService.uploadImages(created.id, pendingImages);
          } catch (imageError) {
            addToast({ title: "Erreur", description: getErrorMessage(imageError, "Le véhicule est enregistré, mais les photos n'ont pas pu être envoyées."), type: "error" });
          }
        }
      }
      addToast({ title: "Succès", description: vehicle ? "Véhicule mis à jour." : "Véhicule ajouté.", type: "success" });
      onSaved();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'enregistrer le véhicule."), type: "error" });
      setStep("form");
    }
  };

  const submit = handleSubmit((values) => (vehicle ? save(values) : setStep("recap")));

  return (
    <Dialog isOpen onClose={onClose} title={vehicle ? "Modifier le véhicule" : step === "form" ? "Ajouter un véhicule" : "Récapitulatif"}
      description={!vehicle && step === "recap" ? "Vérifiez les informations avant d'enregistrer." : undefined} className="max-w-xl">
      {step === "recap" ? (
        <div className="space-y-4">
          <VehicleRecap values={getValues()} />
          <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
            <Button variant="outline" onClick={() => setStep("form")}>Modifier</Button>
            <Button isLoading={formState.isSubmitting} onClick={() => handleSubmit(save)()}>Enregistrer le véhicule</Button>
          </div>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FormField label="Numéro du véhicule *" error={errors.fleetNumber?.message} {...register("fleetNumber")} />
            <FormField label="Immatriculation *" error={errors.plate?.message} {...register("plate")} />
            <FormField label="Marque *" error={errors.brand?.message} {...register("brand")} />
            <FormField label="Modèle *" error={errors.model?.message} {...register("model")} />
            <FormField label="Année *" inputMode="numeric" maxLength={4} placeholder="2024" error={errors.year?.message} {...register("year")} />
            <Controller control={control} name="color" render={({ field }) => <ColorPicker label="Couleur *" value={field.value} error={errors.color?.message} onChange={field.onChange} />} />
            <FormField label="Prix d'achat (FCFA) *" inputMode="numeric" error={errors.purchasePrice?.message} {...register("purchasePrice")} />
            <FormField label="Loyer journalier (FCFA) *" inputMode="numeric" error={errors.dailyRent?.message} {...register("dailyRent")} />
          </div>
          <FormField label="N° de série (VIN)" hint="Facultatif." error={errors.vin?.message} {...register("vin")} />
          <VehicleImagesField vehicleId={vehicle?.id ?? null} pendingFiles={pendingImages} onPendingFilesChange={setPendingImages} />
          <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
            <Button variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" isLoading={formState.isSubmitting}>{vehicle ? "Enregistrer" : "Continuer"}</Button>
          </div>
        </form>
      )}
    </Dialog>
  );
}
