import { z } from "zod";

export const PHONE = /^[+0-9 ().-]{6,30}$/;

export const contactSchema = z.object({
  name: z.string().trim().min(1, "Indiquez votre nom.").max(100),
  email: z.union([z.literal(""), z.email("Adresse e-mail invalide.")]),
  phone: z.union([z.literal(""), z.string().regex(PHONE, "Numéro de téléphone invalide.")]),
  subject: z.string().trim().min(1, "Indiquez l'objet de votre message.").max(150),
  message: z.string().trim().min(1, "Écrivez votre message.").max(2000, "2000 caractères au maximum."),
  website: z.string().max(200),
});

export type ContactValues = z.infer<typeof contactSchema>;
