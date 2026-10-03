import type { Metadata } from "next";
import Image from "next/image";
import { Container } from "@/components/ui/container";
import { RoadLine } from "@/components/ui/road-line";
import { ApplicationForm } from "@/features/apply/components/application-form";

export const metadata: Metadata = {
  title: "Devenir chauffeur partenaire",
  description: "Déposez votre candidature pour louer un véhicule électrique Ndzela et rouler en VTC à Libreville.",
  alternates: { canonical: "/devenir-chauffeur" },
};

export default function ApplyPage() {
  return (
    <>
      <section className="bg-sky py-16 sm:py-20">
        <Container className="grid items-center gap-10 lg:grid-cols-[1.1fr_1fr]">
          <div>
            <h1 className="font-display text-4xl font-bold tracking-tight text-balance sm:text-5xl">Devenez chauffeur partenaire Ndzela</h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted">Remplissez ce formulaire avec vos informations et vos documents. Notre équipe étudie votre dossier et vous rappelle pour la suite.</p>
            <RoadLine className="mt-10 max-w-xs" />
          </div>
          <div className="overflow-hidden rounded-[2rem] rounded-tr-[4rem] shadow-xl ring-1 ring-ink/10">
            <Image src="/images/cars/car1.jpeg" alt="Chauffeur Ndzela au volant d'une voiture électrique" width={1280} height={853} priority sizes="(min-width: 1024px) 480px, 100vw" className="h-auto w-full" />
          </div>
        </Container>
      </section>
      <section className="py-16 sm:py-20">
        <Container className="max-w-3xl"><ApplicationForm /></Container>
      </section>
    </>
  );
}
