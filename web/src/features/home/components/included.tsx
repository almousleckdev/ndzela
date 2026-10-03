import { Compass, ShieldCheck, Smartphone, Wallet, Wrench, Zap, type LucideIcon } from "lucide-react";
import { Container } from "@/components/ui/container";
import { SectionHeading } from "@/components/ui/section-heading";

const ITEMS: { icon: LucideIcon; title: string; text: string }[] = [
  { icon: Wrench, title: "Entretien programmé", text: "Plaquettes et disques, pneumatiques, amortisseurs et batterie sont entretenus par Ndzela." },
  { icon: ShieldCheck, title: "Assurance responsabilité civile", text: "Le véhicule est couvert par une assurance RC souscrite par Ndzela." },
  { icon: Zap, title: "Recharge à la station Ndzela", text: "Rechargez avec l'application de recharge : vous scannez la borne et vous payez ce que vous rechargez." },
  { icon: Compass, title: "Géolocalisation", text: "Le suivi du véhicule protège la flotte et facilite l'assistance." },
  { icon: Wallet, title: "Paiement par Airtel Money et Moov Money", text: "Réglez votre loyer en quelques secondes depuis l'application, avec votre compte Airtel Money ou Moov Money." },
  { icon: Smartphone, title: "Application chauffeur", text: "Suivez votre contrat, payez votre loyer et gardez vos reçus depuis votre téléphone." },
];

export function Included() {
  return (
    <section className="py-20 sm:py-28">
      <Container>
        <SectionHeading title="Ce qui est inclus dans votre contrat" description="Moins de soucis pour le chauffeur, plus de temps sur la route." />
        <ul className="mt-12 grid gap-x-10 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
          {ITEMS.map(({ icon: Icon, title, text }) => (
            <li key={title} className="flex gap-4">
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sky text-action"><Icon className="h-6 w-6" aria-hidden /></span>
              <div>
                <h3 className="font-display text-lg font-bold">{title}</h3>
                <p className="mt-1.5 leading-relaxed text-muted">{text}</p>
              </div>
            </li>
          ))}
        </ul>
      </Container>
    </section>
  );
}
