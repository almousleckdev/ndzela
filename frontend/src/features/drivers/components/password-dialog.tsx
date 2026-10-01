"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { passwordSchema, type PasswordValues } from "@/features/drivers/schemas/driver";

interface PasswordDialogProps {
  email: string;
  onClose: () => void;
  onConfirm: (password: string) => Promise<void>;
}

/** Last step: the initial password of the driver's account. */
export function PasswordDialog({ email, onClose, onConfirm }: PasswordDialogProps) {
  const { register, handleSubmit, formState } = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema), defaultValues: { password: "", confirmation: "" } });

  return (
    <Dialog isOpen onClose={onClose} title="Compte du chauffeur"
      description="Le chauffeur se connectera avec son email ou son téléphone et ce mot de passe, qu'il devra changer à sa première connexion.">
      <form onSubmit={handleSubmit(({ password }) => onConfirm(password))} className="space-y-4" noValidate>
        <FormField label="Email" value={email} readOnly disabled className="bg-slate-50 text-slate-500" />
        <FormField label="Mot de passe" type="password" autoComplete="new-password" hint="8 caractères minimum." error={formState.errors.password?.message} {...register("password")} />
        <FormField label="Confirmer le mot de passe" type="password" autoComplete="new-password" error={formState.errors.confirmation?.message} {...register("confirmation")} />
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Retour</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Créer le chauffeur</Button>
        </div>
      </form>
    </Dialog>
  );
}
