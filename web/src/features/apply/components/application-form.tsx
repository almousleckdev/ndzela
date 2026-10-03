"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { FileField } from "@/components/ui/file-field";
import { InputField, TextareaField } from "@/components/ui/field";
import { SendingScreen } from "@/components/ui/sending-screen";
import { SubmitButton } from "@/components/ui/submit-button";
import { submitToApi, SubmitError } from "@/lib/api";
import { applicationSchema, DOCUMENT_TYPES, fileProblem, IMAGE_TYPES, type ApplicationValues } from "@/features/apply/schema";

type FileKey = "photo" | "licence" | "id" | "addressProof";
type Files = Record<FileKey, File | null>;

const EMPTY: ApplicationValues = { firstName: "", lastName: "", phone: "", email: "", birthDate: "", city: "", address: "", licenceNumber: "", message: "", consent: false, website: "" };
const NO_FILES: Files = { photo: null, licence: null, id: null, addressProof: null };
const REQUIRED: FileKey[] = ["photo", "licence", "id"];

function Section({ title, description, children }: { title: string; description?: string; children: React.ReactNode }) {
  return (
    <fieldset className="space-y-5 border-t border-line pt-8 first:border-0 first:pt-0">
      <legend className="font-display text-xl font-bold">{title}</legend>
      {description && <p className="-mt-2 text-sm text-muted">{description}</p>}
      {children}
    </fieldset>
  );
}

export function ApplicationForm() {
  const [files, setFiles] = useState<Files>(NO_FILES);
  const [fileErrors, setFileErrors] = useState<Partial<Record<FileKey, string>>>({});
  const [sent, setSent] = useState(false);
  const [failure, setFailure] = useState<string>();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ApplicationValues>({ resolver: zodResolver(applicationSchema), defaultValues: EMPTY });

  const pick = (key: FileKey, allowed: string[]) => (file: File | null) => {
    setFiles((current) => ({ ...current, [key]: file }));
    setFileErrors((current) => ({ ...current, [key]: file ? fileProblem(file, allowed) : undefined }));
  };

  const onSubmit = handleSubmit(async (values) => {
    setFailure(undefined);
    const missing = Object.fromEntries(REQUIRED.filter((key) => !files[key]).map((key) => [key, "Ce document est obligatoire."]));
    const invalid = Object.values(fileErrors).some(Boolean);
    if (Object.keys(missing).length > 0 || invalid) {
      setFileErrors((current) => ({ ...current, ...missing }));
      return;
    }
    const { consent: _consent, birthDate, ...rest } = values;
    void _consent;
    const form = new FormData();
    form.append("data", new Blob([JSON.stringify({ ...rest, birthDate: birthDate || undefined })], { type: "application/json" }));
    (Object.keys(files) as FileKey[]).forEach((key) => { const file = files[key]; if (file) form.append(key, file); });
    try {
      await submitToApi("/applications", form);
      reset(EMPTY);
      setFiles(NO_FILES);
      setSent(true);
    } catch (error) {
      setFailure(error instanceof SubmitError ? error.message : "L'envoi a échoué. Réessayez.");
    }
  });

  if (sent) {
    return <SendingScreen title="Candidature envoyée" message="Merci de votre confiance. Notre équipe étudie votre dossier et vous contacte par téléphone." onDone={() => setSent(false)} />;
  }

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-8 rounded-3xl border border-line p-6 sm:p-10">
      <Section title="Vos informations">
        <div className="grid gap-5 sm:grid-cols-2">
          <InputField label="Prénom" required autoComplete="given-name" error={errors.firstName?.message} {...register("firstName")} />
          <InputField label="Nom" required autoComplete="family-name" error={errors.lastName?.message} {...register("lastName")} />
          <InputField label="Téléphone" required type="tel" autoComplete="tel" hint="Nous vous appelons sur ce numéro." error={errors.phone?.message} {...register("phone")} />
          <InputField label="E-mail" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
          <InputField label="Date de naissance" type="date" autoComplete="bday" error={errors.birthDate?.message} {...register("birthDate")} />
          <InputField label="Ville" autoComplete="address-level2" error={errors.city?.message} {...register("city")} />
        </div>
        <InputField label="Adresse" autoComplete="street-address" error={errors.address?.message} {...register("address")} />
      </Section>

      <Section title="Votre permis de conduire">
        <InputField label="Numéro du permis" error={errors.licenceNumber?.message} {...register("licenceNumber")} />
      </Section>

      <Section title="Vos documents" description="Photos nettes et lisibles. JPG, PNG, WebP ou PDF, 5 Mo maximum chacun.">
        <div className="grid gap-5 sm:grid-cols-2">
          <FileField label="Photo de profil" required accept={IMAGE_TYPES.join(",")} file={files.photo} error={fileErrors.photo} onChange={pick("photo", IMAGE_TYPES)} hint="Un portrait de face." />
          <FileField label="Permis de conduire" required accept={DOCUMENT_TYPES.join(",")} file={files.licence} error={fileErrors.licence} onChange={pick("licence", DOCUMENT_TYPES)} />
          <FileField label="Pièce d'identité" required accept={DOCUMENT_TYPES.join(",")} file={files.id} error={fileErrors.id} onChange={pick("id", DOCUMENT_TYPES)} hint="Carte nationale d'identité ou passeport." />
          <FileField label="Casier judiciaire" accept={DOCUMENT_TYPES.join(",")} file={files.addressProof} error={fileErrors.addressProof} onChange={pick("addressProof", DOCUMENT_TYPES)} hint="Extrait de casier judiciaire. Facultatif." />
        </div>
      </Section>

      <Section title="Un mot pour nous">
        <TextareaField label="Message" hint="Votre expérience de conduite, vos disponibilités, vos questions." error={errors.message?.message} {...register("message")} />
      </Section>

      <div className="absolute -left-[9999px] h-0 w-0 overflow-hidden" aria-hidden><label>Site web<input tabIndex={-1} autoComplete="off" {...register("website")} /></label></div>

      <div className="space-y-5 border-t border-line pt-8">
        <div>
          <label className="flex items-start gap-3 text-sm leading-relaxed">
            <input type="checkbox" className="mt-1 h-4 w-4 shrink-0 accent-action" {...register("consent")} />
            <span>J&apos;accepte que Ndzela utilise ces informations et ces documents pour étudier ma candidature.</span>
          </label>
          {errors.consent && <p role="alert" className="mt-1.5 text-sm font-medium text-red-700">{errors.consent.message}</p>}
        </div>
        {failure && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm font-medium text-red-800">{failure}</p>}
        <SubmitButton loading={isSubmitting} className="w-full sm:w-auto">Envoyer ma candidature</SubmitButton>
      </div>
    </form>
  );
}
