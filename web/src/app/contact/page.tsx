import type { Metadata } from "next";
import Image from "next/image";
import { Container } from "@/components/ui/container";
import { RoadLine } from "@/components/ui/road-line";
import { ContactDetails } from "@/features/contact/components/contact-details";
import { ContactForm } from "@/features/contact/components/contact-form";
import { OpeningHours } from "@/features/contact/components/opening-hours";

export const metadata: Metadata = {
  title: "Contact",
  description: "Contactez Ndzela à Libreville : formulaire, téléphone, WhatsApp, e-mail, adresse et horaires d'ouverture.",
  alternates: { canonical: "/contact" },
};

export default function ContactPage() {
  return (
    <>
      <section className="bg-sky py-16 sm:py-20">
        <Container className="grid items-center gap-10 lg:grid-cols-[1.1fr_1fr]">
          <div>
            <h1 className="font-display text-4xl font-bold tracking-tight sm:text-5xl">Contactez-nous</h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted">Une question sur la location d&apos;un véhicule ou sur notre station de recharge ? Écrivez-nous : notre équipe vous répond pendant les heures d&apos;ouverture.</p>
            <RoadLine className="mt-10 max-w-xs" />
          </div>
          <div className="overflow-hidden rounded-[2rem] rounded-tr-[4rem] shadow-xl ring-1 ring-ink/10">
            <Image src="/images/cars/site.jpeg" alt="Flotte de voitures électriques Ndzela à la station de recharge de Libreville" width={1280} height={853} priority sizes="(min-width: 1024px) 480px, 100vw" className="h-auto w-full" />
          </div>
        </Container>
      </section>
      <section className="py-16 sm:py-20">
        <Container className="grid gap-8 lg:grid-cols-[1.15fr_1fr] lg:items-start">
          <ContactForm />
          <div className="space-y-6">
            <ContactDetails />
            <OpeningHours />
          </div>
        </Container>
      </section>
    </>
  );
}
