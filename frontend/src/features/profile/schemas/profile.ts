import { z } from "zod";

export const profileSchema = z.object({
  firstName: z.string().max(100, "Le prénom est trop long"),
  lastName: z.string().max(100, "Le nom est trop long"),
  phoneNumber: z.string().max(50, "Le numéro de téléphone est trop long"),
});

export type ProfileValues = z.infer<typeof profileSchema>;
