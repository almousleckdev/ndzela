"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Smartphone } from "lucide-react";
import { z } from "zod";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { CodeInput } from "@/components/ui/code-input";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { authService } from "@/features/auth/services/auth.service";
import { resetMfa } from "@/features/profile/services/profile.service";

const resetSchema = z.object({
  password: z.string().min(1, "Le mot de passe est requis"),
  code: z.string().regex(/^\d{6}$/, "Le code doit contenir 6 chiffres"),
});
type ResetValues = z.infer<typeof resetSchema>;

function ResetMfaDialog({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const { addToast } = useToast();
  const { register, handleSubmit, formState } = useForm<ResetValues>({ resolver: zodResolver(resetSchema) });

  const submit = async ({ password, code }: ResetValues) => {
    try {
      await resetMfa(password, code);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Mot de passe ou code incorrect."), type: "error" });
      return;
    }
    addToast({ title: "Succès", description: "Authentificateur réinitialisé. Reconnectez-vous pour configurer le nouveau.", type: "success" });
    await authService.logout().catch(() => undefined);
    router.replace("/login");
  };

  return (
    <Dialog isOpen onClose={onClose} title="Réinitialiser l'authentificateur"
      description="Pour changer de téléphone. Confirmez avec votre mot de passe et le code actuel de votre application. Vous serez déconnecté et configurerez la nouvelle application à la prochaine connexion.">
      <form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        <FormField label="Mot de passe" type="password" autoComplete="current-password" error={formState.errors.password?.message} {...register("password")} />
        <div>
          <p className="mb-1.5 text-sm font-medium text-slate-800">Code de l&apos;application</p>
          <CodeInput error={formState.errors.code?.message} {...register("code")} />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" onClick={onClose}>Annuler</Button>
          <Button type="submit" variant="destructive" isLoading={formState.isSubmitting}>Réinitialiser</Button>
        </div>
      </form>
    </Dialog>
  );
}

/**
 * Two-factor authentication is mandatory, so it cannot be switched off. What a user can do is replace
 * the authenticator (new phone), which is protected by password + current code.
 */
export function MfaStatusCard({ mfaEnabled }: { mfaEnabled: boolean }) {
  const [isResetting, setIsResetting] = useState(false);

  return (
    <Card className="lg:p-8">
      <div className="flex items-start justify-between gap-4">
        <CardTitle icon={Smartphone}>Authentification à Deux Facteurs</CardTitle>
        <Badge variant={mfaEnabled ? "success" : "neutral"}>{mfaEnabled ? "Activée" : "À configurer"}</Badge>
      </div>
      <p className="text-sm text-slate-600">
        Chaque connexion demande un code envoyé par email puis un code de votre application d&apos;authentification.
        Cette protection est obligatoire pour tous les comptes et ne peut pas être désactivée.
        {!mfaEnabled && " Vous configurerez votre application lors de votre prochaine connexion."}
      </p>
      {mfaEnabled && (
        <Button variant="outline" className="mt-5" onClick={() => setIsResetting(true)}>
          Changer de téléphone (réinitialiser)
        </Button>
      )}
      {isResetting && <ResetMfaDialog onClose={() => setIsResetting(false)} />}
    </Card>
  );
}
