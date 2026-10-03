import Image from "next/image";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { Container } from "@/components/ui/container";
import { SectionHeading } from "@/components/ui/section-heading";

const OFFERS = [
  { href: "/#location", image: "/images/cars/car.jpeg", alt: "Voiture électrique Ndzela, portières ouvertes", title: "Louer pour travailler", text: "Un véhicule électrique prêt à rouler en VTC, avec un loyer quotidien fixe et la possibilité de devenir propriétaire à la fin du contrat.", cta: "Voir le programme" },
  { href: "/#recharge", image: "/images/charge/char.jpeg", alt: "La station de recharge Ndzela, bornes sous un abri", title: "Recharger à notre station", text: "Ndzela exploite une station de recharge, ouverte aux chauffeurs partenaires comme au public. Scannez la borne avec l'application et payez ce que vous rechargez.", cta: "Découvrir la station" },
];

export function Offers() {
  return (
    <section className="py-20 sm:py-28">
      <Container>
        <SectionHeading title="Deux façons de rouler avec Ndzela" description="Un programme de location pour les chauffeurs, une station de recharge pour tout le monde." />
        <div className="mt-12 grid gap-6 lg:grid-cols-2">
          {OFFERS.map((offer) => (
            <Link key={offer.href} href={offer.href} className="group overflow-hidden rounded-3xl border border-line transition-shadow hover:shadow-xl">
              <div className="overflow-hidden">
                <Image src={offer.image} alt={offer.alt} width={1280} height={853} sizes="(min-width: 1024px) 560px, 100vw" className="aspect-[3/2] w-full object-cover transition-transform duration-500 group-hover:scale-105" />
              </div>
              <div className="p-8">
                <h3 className="font-display text-2xl font-bold">{offer.title}</h3>
                <p className="mt-3 leading-relaxed text-muted">{offer.text}</p>
                <span className="mt-6 inline-flex items-center gap-2 font-semibold text-action">{offer.cta} <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" aria-hidden /></span>
              </div>
            </Link>
          ))}
        </div>
      </Container>
    </section>
  );
}
