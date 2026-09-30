"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { roleLabel } from "@/features/access/lib/permissions";
import { userSchema, type UserValues } from "@/features/users/schemas/user";
import { usersService } from "@/features/users/services/users.service";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { UserSummary } from "@/features/users/types";
import type { RoleSummary } from "@/features/roles/types";

interface UserFormDialogProps {
  /** null = invite a new user, otherwise edit that user. */
  user: UserSummary | null;
  roles: RoleSummary[];
  onClose: () => void;
  onSaved: () => void;
}

export function UserFormDialog({ user, roles, onClose, onSaved }: UserFormDialogProps) {
  const { addToast } = useToast();
  const me = useAuthStore((state) => state.user);
  const isSelf = user !== null && user.email === me?.email;
  const [roleIds, setRoleIds] = useState<string[]>(user?.roles.map((r) => r.id) ?? []);
  const [roleError, setRoleError] = useState<string>();

  const { register, handleSubmit, formState } = useForm<UserValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      email: user?.email ?? "",
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      phoneNumber: user?.phoneNumber ?? "",
    },
  });

  const toggleRole = (id: string) => {
    setRoleError(undefined);
    setRoleIds((current) => (current.includes(id) ? current.filter((r) => r !== id) : [...current, id]));
  };

  const save = async ({ email, ...profile }: UserValues) => {
    if (roleIds.length === 0) {
      setRoleError("Choisissez au moins un rôle.");
      return;
    }
    try {
      const input = { ...profile, roleIds };
      if (user) await usersService.update(user.id, input);
      else await usersService.create(email, input);
      addToast({
        title: "Succès",
        description: user ? "Utilisateur mis à jour." : "Utilisateur créé. Une invitation lui a été envoyée par email.",
        type: "success",
      });
      onSaved();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'enregistrer l'utilisateur."), type: "error" });
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title={user ? "Modifier l'utilisateur" : "Inviter un utilisateur"}
      description={user ? undefined : "L'utilisateur recevra un email pour choisir son mot de passe, puis configurera l'authentification à deux facteurs."}
      className="max-w-xl">
      <form onSubmit={handleSubmit(save)} className="space-y-4" noValidate>
        <FormField label="Adresse Email" type="email" disabled={user !== null} error={formState.errors.email?.message} {...register("email")} />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Prénom" error={formState.errors.firstName?.message} {...register("firstName")} />
          <FormField label="Nom" error={formState.errors.lastName?.message} {...register("lastName")} />
        </div>
        <FormField label="Téléphone" type="tel" error={formState.errors.phoneNumber?.message} {...register("phoneNumber")} />

        <fieldset>
          <Label asLegend>Rôles</Label>
          <div className="max-h-48 overflow-y-auto rounded-lg border border-slate-200 p-1">
            {roles.map((role) => (
              <Checkbox
                key={role.id}
                label={roleLabel(role.name)}
                description={`${role.permissions.length} permission(s)`}
                checked={roleIds.includes(role.id)}
                disabled={isSelf}
                onChange={() => toggleRole(role.id)}
              />
            ))}
          </div>
          {isSelf && <p className="mt-1 text-xs text-slate-500">Vous ne pouvez pas modifier vos propres rôles.</p>}
          {roleError && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{roleError}</p>}
        </fieldset>

        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>{user ? "Enregistrer" : "Envoyer l'invitation"}</Button>
        </div>
      </form>
    </Dialog>
  );
}
