"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, CheckCircle2, KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { resetPasswordSchema, type ResetPasswordValues } from "@/features/auth/schemas/auth";
import { authService } from "@/features/auth/services/auth.service";
import { StepHeader } from "@/features/auth/components/step-header";

export function ResetPasswordForm() {
  const [isDone, setIsDone] = useState(false);
  const token = useSearchParams().get("token");
  const router = useRouter();
  const { addToast } = useToast();
  const { register, handleSubmit, formState } = useForm<ResetPasswordValues>({ resolver: zodResolver(resetPasswordSchema) });

  const goToLogin = () => router.push("/login");

  const onSubmit = async ({ newPassword }: ResetPasswordValues) => {
    if (!token) {
      addToast({ title: "Erreur", description: "Lien de réinitialisation invalide ou manquant.", type: "error" });
      return;
    }
    try {
      await authService.resetPassword(token, newPassword);
      setIsDone(true);
    } catch (error) {
      addToast({
        title: "Erreur",
        description: getErrorMessage(error, "Ce lien est invalide ou a expiré. Demandez-en un nouveau."),
        type: "error",
      });
    }
  };

  if (isDone) {
    return (
      <div className="space-y-6 text-center">
        <CheckCircle2 className="mx-auto h-12 w-12 text-primary" />
        <StepHeader
          title="Mot de passe modifié"
          description="Votre mot de passe a été réinitialisé. Vous pouvez maintenant vous connecter."
        />
        <Button size="xl" onClick={goToLogin} className="w-full">
          Aller à la connexion
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
      <StepHeader title="Réinitialiser le mot de passe" description="Choisissez votre nouveau mot de passe." />
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
      <Button variant="ghost" onClick={goToLogin} className="w-full font-semibold text-slate-600">
        Annuler
      </Button>
    </form>
  );
}
