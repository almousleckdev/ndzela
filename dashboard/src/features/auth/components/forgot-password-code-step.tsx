"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { CodeInput } from "@/components/ui/code-input";
import { resetPasswordWithCodeSchema, type ResetPasswordWithCodeValues } from "@/features/auth/schemas/auth";
import { StepHeader } from "@/features/auth/components/step-header";

interface ForgotPasswordCodeStepProps {
  onSubmit: (values: ResetPasswordWithCodeValues) => Promise<void>;
  onBack: () => void;
}

// Collects the emailed code and the new password in one step, mirroring the driver app's reset flow.
export function ForgotPasswordCodeStep({ onSubmit, onBack }: ForgotPasswordCodeStepProps) {
  const { register, handleSubmit, formState } = useForm<ResetPasswordWithCodeValues>({ resolver: zodResolver(resetPasswordWithCodeSchema) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
      <StepHeader
        title="Entrez le code"
        description="Si cette adresse est associée à un compte, un code à 6 chiffres a été envoyé par email."
      />
      <CodeInput autoFocus error={formState.errors.code?.message} {...register("code")} />
      <FormField
        label="Nouveau mot de passe"
        icon={KeyRound}
        variant="glass"
        type="password"
        autoComplete="new-password"
        placeholder="••••••••"
        hint="8 caractères minimum, avec majuscule, minuscule, chiffre et caractère spécial."
        error={formState.errors.newPassword?.message}
        {...register("newPassword")}
      />
      <FormField
        label="Confirmer le mot de passe"
        icon={KeyRound}
        variant="glass"
        type="password"
        autoComplete="new-password"
        placeholder="••••••••"
        error={formState.errors.confirmPassword?.message}
        {...register("confirmPassword")}
      />
      <Button type="submit" size="xl" className="w-full" isLoading={formState.isSubmitting}>
        Réinitialiser <ArrowRight className="ml-2 h-4 w-4" />
      </Button>
      <Button variant="ghost" onClick={onBack} className="w-full font-semibold text-slate-600">
        Retour à la connexion
      </Button>
    </form>
  );
}
