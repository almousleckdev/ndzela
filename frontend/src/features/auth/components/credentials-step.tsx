"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, KeyRound, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { credentialsSchema, type CredentialsValues } from "@/features/auth/schemas/auth";

interface CredentialsStepProps {
  onSubmit: (values: CredentialsValues) => Promise<void>;
  onForgotPassword: () => void;
}

export function CredentialsStep({ onSubmit, onForgotPassword }: CredentialsStepProps) {
  const { register, handleSubmit, formState } = useForm<CredentialsValues>({ resolver: zodResolver(credentialsSchema) });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
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
      <FormField
        label="Mot de passe"
        icon={KeyRound}
        variant="glass"
        type="password"
        autoComplete="current-password"
        placeholder="••••••••"
        error={formState.errors.password?.message}
        {...register("password")}
      />

      <div className="flex justify-end">
        <Button variant="link" size="sm" onClick={onForgotPassword} className="font-semibold">
          Mot de passe oublié ?
        </Button>
      </div>

      <Button type="submit" size="xl" className="w-full" isLoading={formState.isSubmitting}>
        Connexion Sécurisée <ArrowRight className="ml-2 h-4 w-4" />
      </Button>
    </form>
  );
}
