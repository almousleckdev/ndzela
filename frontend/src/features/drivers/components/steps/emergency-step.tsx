"use client";

import type { UseFormReturn } from "react-hook-form";
import { FormField } from "@/components/ui/form-field";
import { PhoneField, type PhoneCodeOption } from "@/components/ui/phone-field";
import { SelectField } from "@/components/ui/select-field";
import { RELATIONSHIP_LABELS, toOptions } from "@/features/drivers/lib/driver-labels";
import type { PhoneCodes } from "@/features/drivers/lib/driver-form";
import type { DriverFormValues } from "@/features/drivers/schemas/driver";

interface EmergencyStepProps {
  form: UseFormReturn<DriverFormValues>;
  phoneCodes: PhoneCodeOption[];
  codes: PhoneCodes;
  onCodeChange: (key: keyof PhoneCodes, code: string) => void;
}

/** Step 2: the person to contact in an emergency. */
export function EmergencyStep({ form, phoneCodes, codes, onCodeChange }: EmergencyStepProps) {
  const { register, formState } = form;
  const errors = formState.errors.emergency;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FormField label="Nom *" error={errors?.lastName?.message} {...register("emergency.lastName")} />
        <FormField label="Prénom *" error={errors?.firstName?.message} {...register("emergency.firstName")} />
        <PhoneField label="Téléphone *" codes={phoneCodes} code={codes.emergency} onCodeChange={(code) => onCodeChange("emergency", code)}
          error={errors?.phone?.message} {...register("emergency.phone")} />
        <SelectField label="Lien de parenté *" placeholder="Choisir" options={toOptions(RELATIONSHIP_LABELS)} error={errors?.relationship?.message} {...register("emergency.relationship")} />
      </div>
      <FormField label="Adresse complète *" error={errors?.address?.message} {...register("emergency.address")} />
    </div>
  );
}
