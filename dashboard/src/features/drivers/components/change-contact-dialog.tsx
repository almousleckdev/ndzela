"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { joinPhone } from "@/lib/phone";
import { contactSchema, type ContactValues } from "@/features/drivers/schemas/driver";

export interface ContactChange {
  email?: string;
  phone?: string;
}

interface ChangeContactDialogProps {
  driverName: string;
  currentEmail: string;
  currentPhone: string;
  // The country's dial code ("+241"), added in front of the number typed here
  phoneCode: string | null;
  onClose: () => void;
  onConfirm: (change: ContactChange) => Promise<void>;
}

// A new email and/or phone for a driver who no longer has the old ones
export function ChangeContactDialog({ driverName, currentEmail, currentPhone, phoneCode, onClose, onConfirm }: ChangeContactDialogProps) {
  const { register, handleSubmit, formState } = useForm<ContactValues>({ resolver: zodResolver(contactSchema), defaultValues: { email: "", phone: "" } });
  const submit = ({ email, phone }: ContactValues) => onConfirm({ ...(email ? { email } : {}), ...(phone.trim() ? { phone: joinPhone(phoneCode ?? "", phone) } : {}) });

  return (
    <Dialog isOpen onClose={onClose} title="Modifier l'email ou le téléphone"
      description={`${driverName}. Aujourd'hui : ${currentEmail} · ${currentPhone}. Laissez vide ce qui ne change pas.`}>
      <form onSubmit={handleSubmit(submit)} className="space-y-4" noValidate>
        <FormField label="Nouvel email" type="email" autoComplete="off" error={formState.errors.email?.message} {...register("email")} />
        <FormField label="Nouveau numéro de téléphone" inputMode="tel" autoComplete="off" hint={phoneCode ? `Sans l'indicatif (${phoneCode}).` : undefined}
          error={formState.errors.phone?.message} {...register("phone")} />
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Annuler</Button>
          <Button type="submit" isLoading={formState.isSubmitting}>Enregistrer</Button>
        </div>
      </form>
    </Dialog>
  );
}
