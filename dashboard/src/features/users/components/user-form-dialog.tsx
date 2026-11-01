"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { PhoneField } from "@/components/ui/phone-field";
import { AdditionalPhonesField } from "@/features/users/components/additional-phones-field";
import { CountryAccessField } from "@/features/users/components/country-access-field";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { joinPhone, splitPhone } from "@/lib/phone";
import { roleLabel } from "@/features/access/lib/permissions";
import { userSchema, type UserValues } from "@/features/users/schemas/user";
import { usersService } from "@/features/users/services/users.service";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { countriesService } from "@/features/tenancy/services/countries.service";
import { phoneCodeOptions } from "@/features/tenancy/lib/phone-codes";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import type { UserSummary } from "@/features/users/types";
import type { RoleSummary } from "@/features/roles/types";

interface UserFormDialogProps {
  // null = invite a new user, otherwise edit that user.
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
  const [phoneCodeChoice, setPhoneCodeChoice] = useState<string | null>(null);
  const [extraPhones, setExtraPhones] = useState<string[]>(user?.additionalPhoneNumbers ?? []);

  // Only a global actor may assign one or more countries, or make a user global themselves.
  const meIsGlobal = me?.isGlobal ?? false;
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const countries = useFetch(countriesService.list);
  const headerCountryId = countries.data?.find((country) => country.code === selectedCountry)?.id ?? "";
  const [isGlobalChoice, setIsGlobalChoice] = useState(user?.isGlobal ?? false);
  const [countryIds, setCountryIds] = useState<string[]>(() => {
    if (user) return user.countries.map((country) => country.id);
    return headerCountryId ? [headerCountryId] : [];
  });
  const [countryError, setCountryError] = useState<string>();

  const toggleCountry = (id: string) => {
    setCountryError(undefined);
    setCountryIds((current) => (current.includes(id) ? current.filter((c) => c !== id) : [...current, id]));
  };

  const phoneCodes = phoneCodeOptions(countries.data, me?.country ?? null);
  const knownCodes = [me?.country?.phoneCode, ...phoneCodes.map((option) => option.value)].filter((code): code is string => Boolean(code));
  const initialPhone = splitPhone(user?.phoneNumber ?? null, knownCodes);
  const ownCode = me?.country?.phoneCode ?? phoneCodes[0]?.value ?? "";
  const defaultPhoneCode = user ? initialPhone.code ?? ownCode : ownCode;
  const phoneCode = phoneCodeChoice ?? defaultPhoneCode;
  const phoneCodeList = !phoneCode || phoneCodes.some((option) => option.value === phoneCode) ? phoneCodes : [...phoneCodes, { value: phoneCode, label: phoneCode }];

  const { register, handleSubmit, formState } = useForm<UserValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      email: user?.email ?? "",
      firstName: user?.firstName ?? "",
      lastName: user?.lastName ?? "",
      phoneNumber: initialPhone.number,
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
    if (meIsGlobal && !isGlobalChoice && countryIds.length === 0) {
      setCountryError("Choisissez au moins un pays, ou cochez « Utilisateur global ».");
      return;
    }
    try {
      const input = {
        ...profile,
        phoneNumber: joinPhone(phoneCode, profile.phoneNumber),
        additionalPhoneNumbers: extraPhones.map((phone) => phone.trim()).filter(Boolean),
        roleIds,
        isGlobal: meIsGlobal ? isGlobalChoice : undefined,
        countryIds: meIsGlobal && !isGlobalChoice ? countryIds : undefined,
      };
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
        <PhoneField
          label="Téléphone"
          codes={phoneCodeList}
          code={phoneCode}
          onCodeChange={setPhoneCodeChoice}
          placeholder="62 82 18 55"
          error={formState.errors.phoneNumber?.message}
          {...register("phoneNumber")}
        />

        <AdditionalPhonesField phones={extraPhones} onChange={setExtraPhones} />

        {meIsGlobal && (
          <CountryAccessField countries={countries.data} isGlobal={isGlobalChoice} countryIds={countryIds} onToggleCountry={toggleCountry} error={countryError}
            onGlobalChange={() => { setIsGlobalChoice((current) => !current); setCountryError(undefined); }} />
        )}

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
