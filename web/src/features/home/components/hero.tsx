import Image from "next/image";
import { ButtonLink } from "@/components/ui/button-link";
import { RoadLine } from "@/components/ui/road-line";

export function Hero() {
  return (
    <section className="flex min-h-[calc(100svh-4rem)] w-full items-center overflow-hidden bg-sky">
      <div className="mx-auto grid h-full w-full max-w-[110rem] items-center gap-8 px-4 py-10 sm:px-8 lg:grid-cols-[1fr_1.3fr] lg:px-12">
        <div>
          <h1 className="font-display text-4xl font-bold leading-[1.08] tracking-tight text-balance sm:text-5xl lg:text-6xl">
            La voiture électrique qui travaille pour vous.
          </h1>
          <p className="mt-6 max-w-xl text-lg leading-relaxed text-muted">
            Ndzela loue des véhicules électriques aux chauffeurs VTC de Libreville, avec un contrat qui mène à la propriété de la voiture. Et pour tous les automobilistes, nos propres bornes de recharge.
          </p>
          <div className="mt-9 flex flex-col gap-3 sm:flex-row">
            <ButtonLink href="/devenir-chauffeur" className="h-12 px-6 text-base">Devenir chauffeur partenaire</ButtonLink>
            <ButtonLink href="/#recharge" variant="secondary" className="h-12 px-6 text-base">Recharger ma voiture</ButtonLink>
          </div>
          <RoadLine className="mt-12 max-w-xs" />
        </div>

        <Image src="/appui/hero.png" alt="Les écrans de connexion et d'accueil de l'application Ndzela, avec le solde du loyer" width={1538} height={1356} quality={95} priority sizes="(min-width: 1024px) 60vw, 100vw" className="mx-auto h-auto max-h-[calc(100svh-6rem)] w-full object-contain" />
      </div>
    </section>
  );
}
