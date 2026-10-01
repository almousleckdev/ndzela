import { z } from "zod";

export const DEFAULT_DEPOSIT = "250000";

/** A whole amount in FCFA, at least 1. */
export const depositSchema = z
  .string()
  .trim()
  .regex(/^\d{1,14}$/, "Montant invalide (chiffres uniquement)")
  .refine((value) => Number(value) > 0, "La caution doit être supérieure à 0");
