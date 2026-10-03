import { z } from "zod";
import { passwordSchema, passwordsMatch, passwordsMatchIssue } from "@/features/auth/schemas/password";

const emailSchema = z.email("Adresse email invalide");
const sixDigitCode = z.string().regex(/^\d{6}$/, "Le code doit contenir 6 chiffres");

export const credentialsSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, "Le mot de passe est requis"),
});

export const forgotPasswordSchema = z.object({ email: emailSchema });

export const otpSchema = z.object({ code: sixDigitCode });

export const resetPasswordSchema = z
  .object({ newPassword: passwordSchema, confirmPassword: z.string() })
  .refine(passwordsMatch, passwordsMatchIssue);

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Le mot de passe actuel est requis"),
    newPassword: passwordSchema,
    confirmPassword: z.string(),
  })
  .refine(passwordsMatch, passwordsMatchIssue);

export type CredentialsValues = z.infer<typeof credentialsSchema>;
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;
export type CodeValues = z.infer<typeof otpSchema>;
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;
