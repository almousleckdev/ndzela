import { z } from "zod";
import { nationalNumberSchema } from "@/lib/phone";

export const profileSchema = z.object({
  firstName: z.string().max(100, "Le prénom est trop long"),
  lastName: z.string().max(100, "Le nom est trop long"),
  phoneNumber: nationalNumberSchema,
});

export type ProfileValues = z.infer<typeof profileSchema>;
