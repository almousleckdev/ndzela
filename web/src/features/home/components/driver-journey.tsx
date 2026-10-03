import Image from "next/image";
import { Container } from "@/components/ui/container";
import { SectionHeading } from "@/components/ui/section-heading";

const STEPS = [
  { title: "Constituez votre dossier", text: "Permis de conduire valide, pièce d'identité et casier judiciaire." },
  { title: "Signez le contrat", text: "Le contrat de location fixe le loyer, la durée et vos obligations. Il est établi en deux exemplaires originaux." },
  { title: "Prenez la route", text: "Après le versement de la caution, le véhicule vous est remis avec son procès-verbal et son état des lieux." },
  { title: "Payez chaque jour travaillé", text: "Le loyer est dû du lundi au samedi, le dimanche est un jour de repos. Il se règle depuis l'application, avec Airtel Money ou Moov Money." },
  { title: "Devenez propriétaire", text: "Le chauffeur qui respecte tous ses versements se voit transférer la propriété du véhicule, carte grise comprise." },
];

export function DriverJourney() {
  return (
    <section id="location" className="bg-sky py-20 sm:py-28">
      <Container className="grid gap-14 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="lg:sticky lg:top-28 lg:self-start">
          <SectionHeading title="Du dossier à la propriété, étape par étape" description="Le programme de location Ndzela est pensé pour les chauffeurs VTC de Libreville qui veulent posséder leur outil de travail." />
          <Image src="/images/cars/car1.jpeg" alt="Un chauffeur Ndzela au volant, l'écran de bord allumé" width={1280} height={853} sizes="(min-width: 1024px) 460px, 100vw" className="mt-10 hidden aspect-[4/3] w-full rounded-3xl object-cover shadow-lg lg:block" />
        </div>
        <ol className="space-y-4">
          {STEPS.map((step, index) => (
            <li key={step.title} className="flex gap-5 rounded-2xl bg-canvas p-6 shadow-sm ring-1 ring-ink/5">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-ink font-display text-lg font-bold text-white" aria-hidden>{index + 1}</span>
              <div>
                <h3 className="font-display text-lg font-bold">{step.title}</h3>
                <p className="mt-1.5 leading-relaxed text-muted">{step.text}</p>
              </div>
            </li>
          ))}
        </ol>
      </Container>
    </section>
  );
}
