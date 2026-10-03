"use client";

import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { changePasswordSchema, type ChangePasswordValues } from "@/features/auth/schemas/auth";
import { authService } from "@/features/auth/services/auth.service";
import { changePassword } from "@/features/profile/services/profile.service";

export function ChangePasswordCard() {
  const router = useRouter();
  const { addToast } = useToast();
  const { register, handleSubmit, formState } = useForm<ChangePasswordValues>({ resolver: zodResolver(changePasswordSchema) });

  const onSubmit = async ({ currentPassword, newPassword }: ChangePasswordValues) => {
    try {
      await changePassword(currentPassword, newPassword);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Échec de la modification du mot de passe."), type: "error" });
      return;
    }
    // The server ended every session, so sign in again with the new password.
    addToast({ title: "Succès", description: "Mot de passe modifié. Veuillez vous reconnecter.", type: "success" });
    await authService.logout().catch(() => undefined);
    router.replace("/login");
  };

  return (
    <Card className="lg:p-8">
      <CardTitle icon={Lock}>Changer le mot de passe</CardTitle>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>
        <FormField
          label="Mot de passe actuel"
          type="password"
          autoComplete="current-password"
          error={formState.errors.currentPassword?.message}
          {...register("currentPassword")}
        />
        <FormField
          label="Nouveau mot de passe"
          type="password"
          autoComplete="new-password"
          hint="8 caractères minimum, avec majuscule, minuscule, chiffre et caractère spécial."
          error={formState.errors.newPassword?.message}
          {...register("newPassword")}
        />
        <FormField
          label="Confirmer le nouveau mot de passe"
          type="password"
          autoComplete="new-password"
          error={formState.errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />
        <Button type="submit" isLoading={formState.isSubmitting} className="w-full">
          Mettre à jour le mot de passe
        </Button>
      </form>
    </Card>
  );
}
