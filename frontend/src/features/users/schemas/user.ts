import { z } from "zod";

export const userSchema = z.object({
  email: z.email("Adresse email invalide"),
  firstName: z.string().max(100, "Le prénom est trop long"),
  lastName: z.string().max(100, "Le nom est trop long"),
  phoneNumber: z.string().max(50, "Le numéro de téléphone est trop long"),
});

export type UserValues = z.infer<typeof userSchema>;
