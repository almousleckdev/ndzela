"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { generateTemporaryPassword } from "@/features/drivers/lib/temporary-password";
import { passwordSchema, type PasswordValues } from "@/features/drivers/schemas/driver";

interface TemporaryPasswordDialogProps {
  driverName: string;
  onClose: () => void;
  onConfirm: (password: string) => Promise<void>;
}

// A temporary password chosen by the admin for a driver who forgot theirs; the driver must change it at the next sign-in.
export function TemporaryPasswordDialog({ driverName, onClose, onConfirm }: TemporaryPasswordDialogProps) {
  const { register, handleSubmit, formState, setValue, getValues } = useForm<PasswordValues>({ resolver: zodResolver(passwordSchema), defaultValues: { password: "", confirmation: "" } });
  const [copied, setCopied] = useState(false);

  const generate = () => {
    const password = generateTemporaryPassword();
    setValue("password", password, { shouldValidate: true });
    setValue("confirmation", password, { shouldValidate: true });
    setCopied(false);
  };
  const copy = async () => {
    await navigator.clipboard.writeText(getValues("password"));
    setCopied(true);
  };

  return (
    <Dialog isOpen onClose={onClose} title="Nouveau mot de passe provisoire"
      description={`${driverName}. Communiquez ce mot de passe au chauffeur (par téléphone ou en personne) : il devra le changer à sa prochaine connexion. Ses connexions actuelles sont fermées.`}>
      <form onSubmit={handleSubmit(({ password }) => onConfirm(password))} className="space-y-4" noValidate>
        <FormField label="Mot de passe provisoire" type="text" autoComplete="off" hint="8 caractères minimum." error={formState.errors.password?.message} {...register("password")} />
        <FormField label="Confirmer le mot de passe" type="text" autoComplete="off" error={formState.errors.confirmation?.message} {...register("confirmation")} />
        <div className="flex gap-2">
          <Button type="button" variant="outline" onClick={generate}>Générer</Button>
          <Button type="button" variant="outline" onClick={copy}>{copied ? "Copié" : "Copier"}</Button>
        </div>
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer</Button>
        </div>
      </form>
    </Dialog>
  );
}
