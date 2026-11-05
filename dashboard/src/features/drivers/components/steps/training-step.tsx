"use client";

import type { UseFormReturn } from "react-hook-form";
import { FormField } from "@/components/ui/form-field";
import { DOCUMENT_TYPES, FileField } from "@/components/ui/file-field";
import { SelectField } from "@/components/ui/select-field";
import type { Uploads, UploadKey } from "@/features/drivers/lib/driver-form";
import type { DriverFormValues } from "@/features/drivers/schemas/driver";
import type { TrainingCenter } from "@/features/drivers/types";
import type { Vehicle } from "@/features/vehicles/types";

interface TrainingStepProps {
  form: UseFormReturn<DriverFormValues>;
  centers: TrainingCenter[];
  // Available vehicles to choose from; null hides the vehicle choice (editing a driver).
  vehicles: Vehicle[] | null;
  // The documents picked so far; absent when editing (files are managed on the driver's page).
  uploads?: { files: Uploads; errors: Partial<Record<UploadKey, string>>; onPick: (key: UploadKey, file: File | null, error?: string) => void };
}

// Step 3
export function TrainingStep({ form, centers, vehicles, uploads }: TrainingStepProps) {
  const { register, formState } = form;
  const errors = formState.errors.training;

  return (
    <div className="space-y-6">
      {vehicles && (
        <SelectField
          label="Véhicule à assigner *"
          placeholder={vehicles.length === 0 ? "Aucun véhicule disponible" : "Choisir un véhicule"}
          options={vehicles.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.fleetNumber} · ${vehicle.brand} ${vehicle.model} · ${vehicle.plate}` }))}
          error={formState.errors.vehicleId?.message}
          {...register("vehicleId")}
        />
      )}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Centre de formation" placeholder={centers.length === 0 ? "Aucun centre (facultatif)" : "Choisir (facultatif)"}
          options={centers.map((center) => ({ value: center.id, label: center.name }))} error={errors?.trainingCenterId?.message} {...register("training.trainingCenterId")} />
        <FormField label="Date de formation *" type="date" max={new Date().toISOString().slice(0, 10)} error={errors?.trainingDate?.message} {...register("training.trainingDate")} />
      </div>
      <FormField label="N° de certificat" hint="Facultatif." error={errors?.certificateNumber?.message} {...register("training.certificateNumber")} />
      {uploads && (
        <fieldset className="space-y-3">
          <legend className="mb-1.5 text-sm font-medium text-slate-800">Documents fournis</legend>
          <FileField label="Permis de conduire" accepted={DOCUMENT_TYPES} file={uploads.files.licence} error={uploads.errors.licence} onChange={(file, error) => uploads.onPick("licence", file, error)} />
          <FileField label="Pièce d'identité" accepted={DOCUMENT_TYPES} file={uploads.files.id} error={uploads.errors.id} onChange={(file, error) => uploads.onPick("id", file, error)} />
          <FileField label="Casier judiciaire" accepted={DOCUMENT_TYPES} file={uploads.files.addressProof} error={uploads.errors.addressProof} onChange={(file, error) => uploads.onPick("addressProof", file, error)} />
          <FileField label="Certificat de résidence" accepted={DOCUMENT_TYPES} file={uploads.files.residenceCertificate} error={uploads.errors.residenceCertificate} onChange={(file, error) => uploads.onPick("residenceCertificate", file, error)} />
        </fieldset>
      )}
    </div>
  );
}
