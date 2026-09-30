import { z } from "zod";
import { nationalNumberSchema } from "@/lib/phone";

export const userSchema = z.object({
  email: z.email("Adresse email invalide"),
  firstName: z.string().max(100, "Le prénom est trop long"),
  lastName: z.string().max(100, "Le nom est trop long"),
  phoneNumber: nationalNumberSchema,
});

export type UserValues = z.infer<typeof userSchema>;
