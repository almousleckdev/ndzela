"use client";

import type { UseFormReturn } from "react-hook-form";
import { FileField, IMAGE_TYPES } from "@/components/ui/file-field";
import { FormField } from "@/components/ui/form-field";
import { PhoneField, type PhoneCodeOption } from "@/components/ui/phone-field";
import { SelectField } from "@/components/ui/select-field";
import { CIVILITY_LABELS, ID_TYPE_LABELS, LICENCE_CATEGORIES, toOptions } from "@/features/drivers/lib/driver-labels";
import type { PhotoPick } from "@/features/drivers/lib/driver-form";
import type { PhoneCodes } from "@/features/drivers/lib/driver-form";
import type { DriverFormValues } from "@/features/drivers/schemas/driver";

interface StepProps {
  form: UseFormReturn<DriverFormValues>;
  phoneCodes: PhoneCodeOption[];
  codes: PhoneCodes;
  onCodeChange: (key: keyof PhoneCodes, code: string) => void;
  /** The profile picture to pick; absent when editing (managed on the driver's page). */
  photo?: PhotoPick;
}

function eighteenYearsAgo(): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - 18);
  return date.toISOString().slice(0, 10);
}

const today = () => new Date().toISOString().slice(0, 10);

/** Step 1: identity, identity document, contacts and driving licence. */
export function PersonalStep({ form, phoneCodes, codes, onCodeChange, photo }: StepProps) {
  const { register, formState } = form;
  const errors = formState.errors.personal;

  return (
    <div className="space-y-8">
      {photo && (
        <section className="space-y-4">
          <h3 className="text-sm font-semibold text-slate-900">Photo de profil</h3>
          <FileField label="Photo du chauffeur *" hint="Obligatoire. JPG ou PNG, 5 Mo maximum." accepted={IMAGE_TYPES} file={photo.file} error={photo.error} onChange={photo.onChange} />
        </section>
      )}

      <section className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-900">Identité</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField label="Civilité *" placeholder="Choisir" options={toOptions(CIVILITY_LABELS)} error={errors?.civility?.message} {...register("personal.civility")} />
          <FormField label="Date de naissance *" type="date" max={eighteenYearsAgo()} hint="18 ans minimum." error={errors?.birthDate?.message} {...register("personal.birthDate")} />
          <FormField label="Nom *" error={errors?.lastName?.message} {...register("personal.lastName")} />
          <FormField label="Prénom *" error={errors?.firstName?.message} {...register("personal.firstName")} />
          <FormField label="Lieu de naissance *" error={errors?.birthPlace?.message} {...register("personal.birthPlace")} />
          <FormField label="Nationalité *" error={errors?.nationality?.message} {...register("personal.nationality")} />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-900">Pièce d&apos;identité</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField label="Type de pièce *" placeholder="Choisir" options={toOptions(ID_TYPE_LABELS)} error={errors?.idType?.message} {...register("personal.idType")} />
          <FormField label="Numéro *" error={errors?.idNumber?.message} {...register("personal.idNumber")} />
          <FormField label="Date de délivrance *" type="date" max={today()} error={errors?.idIssueDate?.message} {...register("personal.idIssueDate")} />
          <FormField label="Lieu de délivrance *" error={errors?.idIssuePlace?.message} {...register("personal.idIssuePlace")} />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-900">Coordonnées</h3>
        <FormField label="Adresse *" error={errors?.address?.message} {...register("personal.address")} />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Quartier *" error={errors?.district?.message} {...register("personal.district")} />
          <FormField label="Ville *" error={errors?.city?.message} {...register("personal.city")} />
          <PhoneField label="Téléphone principal *" codes={phoneCodes} code={codes.primary} onCodeChange={(code) => onCodeChange("primary", code)}
            error={errors?.phonePrimary?.message} {...register("personal.phonePrimary")} />
          <PhoneField label="Téléphone secondaire" codes={phoneCodes} code={codes.secondary} onCodeChange={(code) => onCodeChange("secondary", code)}
            error={errors?.phoneSecondary?.message} {...register("personal.phoneSecondary")} />
        </div>
        <FormField label="Email *" type="email" hint="Sert d'identifiant de connexion au chauffeur." error={errors?.email?.message} {...register("personal.email")} />
      </section>

      <section className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-900">Permis de conduire</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <FormField label="Numéro *" error={errors?.licenceNumber?.message} {...register("personal.licenceNumber")} />
          <FormField label="Date de délivrance *" type="date" max={today()} error={errors?.licenceIssueDate?.message} {...register("personal.licenceIssueDate")} />
          <SelectField label="Catégorie *" placeholder="Choisir" options={LICENCE_CATEGORIES.map((value) => ({ value, label: value }))}
            error={errors?.licenceCategory?.message} {...register("personal.licenceCategory")} />
        </div>
      </section>
    </div>
  );
}
