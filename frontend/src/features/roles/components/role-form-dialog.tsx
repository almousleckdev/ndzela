"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { PermissionMatrix } from "@/features/access/components/permission-matrix";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { roleSchema, type RoleValues } from "@/features/roles/schemas/role";
import { rolesService } from "@/features/roles/services/roles.service";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { RoleSummary } from "@/features/roles/types";

interface RoleFormDialogProps {
  /** null = create a new role, otherwise edit that role's permissions. */
  role: RoleSummary | null;
  catalogue: string[];
  onClose: () => void;
  onSaved: (role: RoleSummary) => void;
}

export function RoleFormDialog({ role, catalogue, onClose, onSaved }: RoleFormDialogProps) {
  const { addToast } = useToast();
  const held = useAuthStore((state) => state.user?.permissions ?? []);
  const [permissions, setPermissions] = useState<string[]>(role?.permissions ?? []);
  const { register, handleSubmit, formState } = useForm<RoleValues>({
    resolver: zodResolver(roleSchema),
    defaultValues: { name: role?.name ?? "" },
  });

  const toggle = (permission: string) =>
    setPermissions((current) => (current.includes(permission) ? current.filter((p) => p !== permission) : [...current, permission]));

  const save = async ({ name }: RoleValues) => {
    try {
      const saved = role ? await rolesService.updatePermissions(role.id, permissions) : await rolesService.create(name, permissions);
      addToast({ title: "Succès", description: role ? "Accès du rôle mis à jour." : "Rôle créé.", type: "success" });
      onSaved(saved);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'enregistrer le rôle."), type: "error" });
    }
  };

  return (
    <Dialog
      isOpen
      onClose={onClose}
      title={role ? `Modifier les accès de « ${role.name} »` : "Nouveau rôle"}
      description="Cliquez sur une case pour autoriser ou retirer une action. Les cases verrouillées correspondent à des permissions que vous ne possédez pas."
      className="max-w-2xl"
    >
      <form onSubmit={handleSubmit(save)} className="space-y-5" noValidate>
        {!role && <FormField label="Nom du rôle" placeholder="SUPPORT_NIVEAU_2" hint="Majuscules, chiffres et _ uniquement." error={formState.errors.name?.message} {...register("name")} />}
        <PermissionMatrix catalogue={catalogue} granted={permissions} onToggle={toggle} held={held} />
        <div className="flex items-center justify-between border-t border-slate-200 pt-4">
          <span className="text-sm text-slate-500">{permissions.length} permission(s) sélectionnée(s)</span>
          <div className="flex gap-3">
            <Button variant="outline" onClick={onClose}>Annuler</Button>
            <Button type="submit" isLoading={formState.isSubmitting}>{role ? "Enregistrer" : "Créer le rôle"}</Button>
          </div>
        </div>
      </form>
    </Dialog>
  );
}
