"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Mail, ShieldCheck } from "lucide-react";
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
        description="Entrez votre adresse email pour recevoir un lien de réinitialisation."
      />
      <FormField
        label="Adresse Email"
        icon={Mail}
        variant="glass"
        type="email"
        autoComplete="username"
        placeholder="admin@ndzela.com"
        error={formState.errors.email?.message}
        {...register("email")}
      />
      <Button type="submit" size="xl" className="w-full" isLoading={formState.isSubmitting}>
        Envoyer le lien
      </Button>
      <Button variant="ghost" onClick={onBack} className="w-full font-semibold text-slate-600">
        Retour à la connexion
      </Button>
    </form>
  );
}

export function ForgotPasswordSent({ onBack }: { onBack: () => void }) {
  return (
    <div className="space-y-6 text-center">
      <ShieldCheck className="mx-auto h-12 w-12 text-primary" />
      <StepHeader
        title="Email envoyé"
        description="Si votre adresse email est associée à un compte, vous recevrez un lien de réinitialisation d'ici quelques minutes."
      />
      <Button size="xl" onClick={onBack} className="w-full">
        Retour à la connexion
      </Button>
    </div>
  );
}
