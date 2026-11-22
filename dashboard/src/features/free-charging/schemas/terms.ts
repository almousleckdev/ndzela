import { z } from "zod";

export const CONDITIONS_MAX = 1000;

export const termsSchema = z
  .object({
    startsOn: z.string().min(1, "Date de début obligatoire"),
    endsOn: z.string(),
    conditions: z.string().max(CONDITIONS_MAX, `${CONDITIONS_MAX} caractères maximum`),
  })
  .refine((values) => values.endsOn === "" || values.endsOn >= values.startsOn, {
    path: ["endsOn"],
    message: "La date de fin ne peut pas précéder la date de début",
  });

export type TermsValues = z.infer<typeof termsSchema>;
