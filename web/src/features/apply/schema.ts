import { z } from "zod";
import { PHONE } from "@/features/contact/schema";

export const applicationSchema = z.object({
  firstName: z.string().trim().min(1, "Indiquez votre prénom.").max(100),
  lastName: z.string().trim().min(1, "Indiquez votre nom.").max(100),
  phone: z.string().regex(PHONE, "Numéro de téléphone invalide."),
  email: z.union([z.literal(""), z.email("Adresse e-mail invalide.")]),
  birthDate: z.string().refine((value) => value === "" || new Date(value) < new Date(), "La date de naissance doit être passée."),
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
