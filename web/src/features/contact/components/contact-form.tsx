"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { InputField, TextareaField } from "@/components/ui/field";
import { SendingScreen } from "@/components/ui/sending-screen";
import { SubmitButton } from "@/components/ui/submit-button";
import { submitToApi, SubmitError } from "@/lib/api";
import { contactSchema, type ContactValues } from "@/features/contact/schema";

const EMPTY: ContactValues = { name: "", email: "", phone: "", subject: "", message: "", website: "" };

export function ContactForm() {
  const [sent, setSent] = useState(false);
  const [failure, setFailure] = useState<string>();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<ContactValues>({ resolver: zodResolver(contactSchema), defaultValues: EMPTY });

  const onSubmit = handleSubmit(async (values) => {
    setFailure(undefined);
    try {
      await submitToApi("/contact", values);
      reset(EMPTY);
      setSent(true);
    } catch (error) {
      setFailure(error instanceof SubmitError ? error.message : "L'envoi a échoué. Réessayez.");
    }
  });

  if (sent) {
    return <SendingScreen title="Message envoyé" message="Merci. Notre équipe vous répond pendant les heures d'ouverture." onDone={() => setSent(false)} />;
  }

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-5 rounded-3xl border border-line p-6 sm:p-8">
      <h2 className="font-display text-2xl font-bold">Écrivez-nous</h2>
      <div className="grid gap-5 sm:grid-cols-2">
        <InputField label="Nom" required autoComplete="name" error={errors.name?.message} {...register("name")} />
        <InputField label="Téléphone" type="tel" autoComplete="tel" error={errors.phone?.message} {...register("phone")} />
      </div>
      <InputField label="E-mail" type="email" autoComplete="email" hint="Pour que nous puissions vous répondre." error={errors.email?.message} {...register("email")} />
      <InputField label="Objet" required error={errors.subject?.message} {...register("subject")} />
      <TextareaField label="Message" required error={errors.message?.message} {...register("message")} />
      {/* A trap for robots: people never see or fill it. */}
      <div aria-hidden className="absolute -left-[9999px] h-0 w-0 overflow-hidden"><label>Site web<input tabIndex={-1} autoComplete="off" {...register("website")} /></label></div>
      {failure && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm font-medium text-red-800">{failure}</p>}
      <SubmitButton loading={isSubmitting} className="w-full sm:w-auto">Envoyer le message</SubmitButton>
    </form>
  );
}
