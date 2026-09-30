"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { FormField } from "@/components/ui/form-field";
import { PhoneField } from "@/components/ui/phone-field";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { joinPhone, splitPhone } from "@/lib/phone";
import { profileSchema, type ProfileValues } from "@/features/profile/schemas/profile";
import { updateProfile } from "@/features/profile/services/profile.service";
import { phoneCodeOptions } from "@/features/tenancy/lib/phone-codes";
import { countriesService } from "@/features/tenancy/services/countries.service";
import type { Country, UserProfile } from "@/features/auth/types";

/** Waits for the country list so the stored phone number can be split into its code and number. */
export function ProfileForm({ user }: { user: UserProfile }) {
  const countries = useFetch(countriesService.list);
  if (countries.isLoading) {
    return <Card className="lg:p-8"><Skeleton className="h-64 w-full" /></Card>;
  }
  return <ProfileFormBody user={user} countries={countries.data} />;
}

function ProfileFormBody({ user, countries }: { user: UserProfile; countries: Country[] | null }) {
  const { addToast } = useToast();
  const phoneCodes = phoneCodeOptions(countries, user.country);
  const initialPhone = splitPhone(user.phoneNumber, phoneCodes.map((option) => option.value));
  const [phoneCode, setPhoneCode] = useState(initialPhone.code ?? user.country?.phoneCode ?? phoneCodes[0]?.value ?? "");
  const { register, handleSubmit, formState } = useForm<ProfileValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: user.firstName ?? "",
      lastName: user.lastName ?? "",
      phoneNumber: initialPhone.number,
    },
  });

  const onSubmit = async (values: ProfileValues) => {
    try {
      await updateProfile({ ...values, phoneNumber: joinPhone(phoneCode, values.phoneNumber) });
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

        <PhoneField
          label="Numéro de Téléphone"
          codes={phoneCodes}
          code={phoneCode}
          onCodeChange={setPhoneCode}
          placeholder="62 82 18 55"
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
