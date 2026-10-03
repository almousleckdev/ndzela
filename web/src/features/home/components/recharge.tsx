import Image from "next/image";
import { BatteryCharging, QrCode, Smartphone, Wallet, type LucideIcon } from "lucide-react";
import { Container } from "@/components/ui/container";
import { StoreButtons } from "@/features/app/components/store-buttons";

const STEPS: { icon: LucideIcon; title: string; text: string }[] = [
  { icon: Smartphone, title: "Téléchargez l'application", text: "L'application de recharge Ndzela est gratuite, sur iPhone et sur Android." },
  { icon: QrCode, title: "Scannez la borne", text: "Garez-vous à la station, puis scannez le code de la borne avec l'application." },
  { icon: Wallet, title: "Payez ce que vous rechargez", text: "Vous choisissez le montant à recharger et vous le payez depuis l'application." },
];

export function Recharge() {
  return (
    <section id="recharge" className="bg-ink py-20 text-white sm:py-28">
      <Container>
        <div className="max-w-2xl">
          <BatteryCharging className="h-9 w-9 text-primary" aria-hidden />
          <h2 className="mt-5 font-display text-3xl font-bold tracking-tight text-balance sm:text-4xl">La station de recharge Ndzela, ouverte à tous</h2>
          <p className="mt-5 text-lg leading-relaxed text-slate-300">Ndzela exploite une station de recharge rapide à Libreville. Chauffeur partenaire ou automobiliste, rechargez votre voiture électrique en scannant la borne avec l&apos;application.</p>
        </div>

        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-[1.4fr_1fr]">
          <Image src="/images/charge/char1.jpeg" alt="La station de recharge Ndzela : une rangée de bornes sous un abri" width={1040} height={780} sizes="(min-width: 1024px) 640px, 100vw" className="h-full w-full rounded-3xl object-cover sm:col-span-2 lg:col-span-1 lg:row-span-2" />
          <Image src="/images/charge/char.jpeg" alt="Une borne de la station Ndzela au premier plan, avec une voiture électrique à la recharge" width={1040} height={780} sizes="(min-width: 1024px) 440px, 100vw" className="aspect-[4/3] w-full rounded-3xl object-cover" />
          <Image src="/images/charge/char2.jpeg" alt="Les bornes de la station Ndzela et des voitures électriques en attente" width={1040} height={780} sizes="(min-width: 1024px) 440px, 100vw" className="aspect-[4/3] w-full rounded-3xl object-cover" />
        </div>

        <div className="mt-16">
          <h3 className="font-display text-2xl font-bold">Comment recharger</h3>
          <ol className="mt-8 grid gap-6 md:grid-cols-3">
            {STEPS.map(({ icon: Icon, title, text }, index) => (
              <li key={title} className="rounded-2xl bg-white/5 p-6 ring-1 ring-white/10">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-ink"><Icon className="h-5 w-5" aria-hidden /></span>
                  <span className="text-sm font-semibold text-slate-400">Étape {index + 1}</span>
                </div>
                <h4 className="mt-4 font-display text-lg font-bold">{title}</h4>
                <p className="mt-2 leading-relaxed text-slate-300">{text}</p>
              </li>
            ))}
          </ol>
          <div className="mt-10"><StoreButtons app="charging" /></div>
        </div>
      </Container>
    </section>
  );
}
