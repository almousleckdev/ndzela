import { z } from "zod";
import { PHONE } from "@/features/contact/schema";

export const MIN_AGE = 30;
export const MAX_AGE = 55;
export const MIN_EXPERIENCE = 5;

// The oldest and youngest dates of birth allowed today, as yyyy-mm-dd for a date input.
export function birthDateBounds(today = new Date()): { min: string; max: string } {
  const iso = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  const max = new Date(today.getFullYear() - MIN_AGE, today.getMonth(), today.getDate());
  const min = new Date(today.getFullYear() - MAX_AGE - 1, today.getMonth(), today.getDate() + 1);
  return { min: iso(min), max: iso(max) };
}

// Full years between a yyyy-mm-dd date of birth and today.
function ageOn(birthDate: string): number {
  const born = new Date(birthDate);
  if (Number.isNaN(born.getTime())) return -1;
  const now = new Date();
  const birthdayPassed = now.getMonth() > born.getMonth() || (now.getMonth() === born.getMonth() && now.getDate() >= born.getDate());
  return now.getFullYear() - born.getFullYear() - (birthdayPassed ? 0 : 1);
}

export const applicationSchema = z.object({
  firstName: z.string().trim().min(1, "Indiquez votre prénom.").max(100),
  lastName: z.string().trim().min(1, "Indiquez votre nom.").max(100),
  phone: z.string().regex(PHONE, "Numéro de téléphone invalide."),
  email: z.union([z.literal(""), z.email("Adresse e-mail invalide.")]),
  birthDate: z.string().min(1, "Indiquez votre date de naissance.").refine(
    (value) => { const age = ageOn(value); return age >= MIN_AGE && age <= MAX_AGE; },
    `Les candidats doivent avoir entre ${MIN_AGE} et ${MAX_AGE} ans.`,
  ),
  drivingExperienceYears: z.string().min(1, "Indiquez vos années d'expérience.").refine((value) => Number.isInteger(Number(value)) && Number(value) >= MIN_EXPERIENCE, `Il faut au moins ${MIN_EXPERIENCE} ans d'expérience de conduite.`),
  city: z.string().max(100),
  address: z.string().max(255),
  licenceNumber: z.string().max(50),
  message: z.string().max(2000, "2000 caractères au maximum."),
  consent: z.boolean().refine(Boolean, "Vous devez accepter pour envoyer votre candidature."),
  website: z.string().max(200),
});

export type ApplicationValues = z.infer<typeof applicationSchema>;

export const MAX_FILE_BYTES = 5 * 1024 * 1024;
export const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
export const DOCUMENT_TYPES = [...IMAGE_TYPES, "application/pdf"];

// The reason a chosen file is refused, or undefined when it is fine.
export function fileProblem(file: File, allowed: string[]): string | undefined {
  if (!allowed.includes(file.type)) return allowed.includes("application/pdf") ? "Utilisez une image (JPG, PNG, WebP) ou un PDF." : "Utilisez une image JPG, PNG ou WebP.";
  if (file.size > MAX_FILE_BYTES) return "Le fichier dépasse 5 Mo.";
  return undefined;
}
