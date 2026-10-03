import Image from "next/image";
import { ButtonLink } from "@/components/ui/button-link";
import { Container } from "@/components/ui/container";
import { RoadLine } from "@/components/ui/road-line";

export function Hero() {
  return (
    <section className="overflow-hidden bg-sky">
      <Container className="grid items-center gap-8 py-14 lg:grid-cols-[1fr_1.1fr] lg:py-20">
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

        <Image src="/appui/loginandhome.png" alt="Les écrans de connexion et d'accueil de l'application Ndzela, avec le solde du loyer" width={999} height={1295} priority sizes="(min-width: 1024px) 520px, 90vw" className="mx-auto h-auto max-h-[36rem] w-auto max-w-full" />
      </Container>
    </section>
  );
}
