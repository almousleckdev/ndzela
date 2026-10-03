import Image from "next/image";
import { CircleCheck } from "lucide-react";
import { Container } from "@/components/ui/container";
import { SectionHeading } from "@/components/ui/section-heading";
import { StoreButtons } from "@/features/app/components/store-buttons";

const FEATURES = [
  "Votre solde et vos jours de retard, dès l'ouverture",
  "Paiement du loyer par Airtel Money ou Moov Money",
  "Un reçu à partager pour chaque paiement",
  "Votre contrat, votre véhicule et le support à portée de main",
];

export function AppShowcase() {
  return (
    <section id="application" className="overflow-x-clip py-20 sm:py-28">
      <Container>
        <div className="grid items-center gap-10 lg:grid-cols-[1fr_1fr]">
          <div>
            <SectionHeading title="Votre loyer, simplement, dans votre poche" description="L'application Ndzela est réservée aux chauffeurs partenaires : elle réunit le paiement du loyer, le suivi du contrat et le support." />
            <ul className="mt-8 space-y-3">
              {FEATURES.map((feature) => (
                <li key={feature} className="flex gap-3"><CircleCheck className="mt-0.5 h-5 w-5 shrink-0 text-action" aria-hidden /><span>{feature}</span></li>
              ))}
            </ul>
            <div className="mt-10"><StoreButtons /></div>
            <p className="mt-4 text-sm text-muted">Pour iPhone et Android. Le compte est créé par Ndzela avec votre contrat.</p>
          </div>
          <div>
            <Image src="/appui/paymentandsuccess.png" alt="Écran de paiement du loyer et détails d'un paiement confirmé dans l'application Ndzela" width={1070} height={1257} sizes="(min-width: 1024px) 480px, 90vw" className="mx-auto h-auto max-h-[36rem] w-auto max-w-full" />
          </div>
        </div>

        <div className="mt-20 grid items-center gap-10 lg:grid-cols-[1fr_1fr]">
          <div className="order-last lg:order-first">
            <Image src="/appui/profile.png" alt="Écran du profil du chauffeur avec ses informations et son contrat dans l'application Ndzela" width={972} height={1300} sizes="(min-width: 1024px) 420px, 90vw" className="mx-auto h-auto max-h-[32rem] w-auto max-w-full" />
          </div>
          <div>
            <h3 className="font-display text-3xl font-bold tracking-tight text-balance">Votre contrat, toujours avec vous</h3>
            <p className="mt-4 text-lg leading-relaxed text-muted">Retrouvez vos informations personnelles, les dates de votre contrat, votre véhicule et vos documents, et contactez le support en un geste.</p>
          </div>
        </div>
      </Container>
    </section>
  );
}
