"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { profileSchema, type ProfileValues } from "@/features/profile/schemas/profile";
import { updateProfile } from "@/features/profile/services/profile.service";
import type { UserProfile } from "@/features/auth/types";

export function ProfileForm({ user }: { user: UserProfile }) {
  const { addToast } = useToast();
  const { register, handleSubmit, formState } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user.firstName ?? "",
      lastName: user.lastName ?? "",
      phoneNumber: user.phoneNumber ?? "",
    },
  });

  const onSubmit = async (values: ProfileValues) => {
    try {
      await updateProfile(values);
      addToast({ title: "Succès", description: "Profil mis à jour avec succès.", type: "success" });
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Échec de la mise à jour du profil."), type: "error" });
    }
  };

  return (
    <Card className="lg:p-8">
      <h3 className="mb-6 font-display text-xl font-semibold text-slate-900">Informations personnelles</h3>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <FormField label="Prénom" placeholder="Entrer le prénom" error={formState.errors.firstName?.message} {...register("firstName")} />
          <FormField label="Nom" placeholder="Entrer le nom" error={formState.errors.lastName?.message} {...register("lastName")} />
        </div>

        <FormField label="Adresse Email" value={user.email} disabled readOnly hint="L'email ne peut pas être modifié." className="bg-slate-50 text-slate-500" />

        <FormField
          label="Numéro de Téléphone"
          type="tel"
          placeholder="+223 00 00 00 00"
          error={formState.errors.phoneNumber?.message}
          {...register("phoneNumber")}
        />

        <div className="flex justify-end border-t border-slate-200 pt-4">
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer les modifications</Button>
        </div>
      </form>
    </Card>
  );
}
