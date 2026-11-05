"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { forgotPasswordSchema, type ForgotPasswordValues } from "@/features/auth/schemas/auth";
import { StepHeader } from "@/features/auth/components/step-header";

interface ForgotPasswordStepProps {
  onSubmit: (values: ForgotPasswordValues) => Promise<void>;
  onBack: () => void;
}

export function ForgotPasswordStep({ onSubmit, onBack }: ForgotPasswordStepProps) {
  const { register, handleSubmit, formState } = useForm<ForgotPasswordValues>({ resolver: zodResolver(forgotPasswordSchema) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
      <StepHeader
        title="Mot de passe oublié"
        description="Entrez votre adresse email pour recevoir un code de réinitialisation."
      />
      <FormField
        label="Adresse Email"
        icon={Mail}
        type="email"
        autoComplete="username"
        placeholder="admin@ndzela.com"
        error={formState.errors.email?.message}
        {...register("email")}
      />
      <Button type="submit" size="xl" className="w-full" isLoading={formState.isSubmitting}>
        Envoyer le code
      </Button>
      <Button variant="ghost" onClick={onBack} className="w-full font-semibold text-slate-600">
        Retour à la connexion
      </Button>
    </form>
  );
}
