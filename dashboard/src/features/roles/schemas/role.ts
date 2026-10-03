import { z } from "zod";

// Same rule as the backend
export const roleSchema = z.object({
  name: z.string().regex(/^[A-Z][A-Z0-9_]{2,49}$/, "3 à 50 caractères : majuscules, chiffres et _ (ex. SUPPORT_AGENT)"),
});

export type RoleValues = z.infer<typeof roleSchema>;
