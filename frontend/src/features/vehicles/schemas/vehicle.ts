import { z } from "zod";

const amount = z
  .string()
  .trim()
  .regex(/^\d{1,14}$/, "Montant invalide (chiffres uniquement)")
  .refine((value) => Number(value) > 0, "Le montant doit être supérieur à 0");

export const vehicleSchema = z.object({
  fleetNumber: z.string().trim().min(1, "Le numéro du véhicule est requis").max(30, "30 caractères maximum"),
  brand: z.string().trim().min(1, "La marque est requise").max(60, "60 caractères maximum"),
  model: z.string().trim().min(1, "Le modèle est requis").max(60, "60 caractères maximum"),
  year: z
    .string()
    .trim()
    .regex(/^\d{4}$/, "Année invalide")
    .refine((value) => Number(value) >= 1990 && Number(value) <= new Date().getFullYear() + 1, "Année hors limites"),
  plate: z.string().trim().min(1, "L'immatriculation est requise").max(20, "20 caractères maximum"),
  color: z.string().trim().min(1, "La couleur est requise").max(30, "30 caractères maximum"),
  vin: z.string().trim().max(30, "30 caractères maximum"),
  purchasePrice: amount,
  dailyRent: amount,
});

export type VehicleValues = z.infer<typeof vehicleSchema>;
