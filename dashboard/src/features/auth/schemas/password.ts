import { z } from "zod";

// Mirrors the backend @StrongPassword rule.
export const passwordSchema = z
  .string()
  .min(8, "Au moins 8 caractères")
  .max(128, "128 caractères maximum")
  .regex(/[0-9]/, "Au moins un chiffre")
  .regex(/[a-z]/, "Au moins une minuscule")
  .regex(/[A-Z]/, "Au moins une majuscule")
  .regex(/[^A-Za-z0-9]/, "Au moins un caractère spécial");

export const passwordsMatch = (data: { newPassword: string; confirmPassword: string }) =>
  data.newPassword === data.confirmPassword;

export const passwordsMatchIssue = {
  message: "Les mots de passe ne correspondent pas",
  path: ["confirmPassword"],
};
