import Image from "next/image";
import Link from "next/link";
import { Mail, MapPin, MessageCircle, Phone } from "lucide-react";
import { StoreButtons } from "@/features/app/components/store-buttons";
import { Container } from "@/components/ui/container";
import { Wordmark } from "@/components/ui/wordmark";
import { RoadLine } from "@/components/ui/road-line";
import { legalLinks, mapsHref, phoneHref, siteConfig, whatsappHref } from "@/config/site";

export function SiteFooter() {
  const { contact } = siteConfig;
  return (
    <footer className="bg-ink text-slate-300">
      <Container className="grid gap-12 py-16 md:grid-cols-[1.4fr_1fr_1.2fr_1.2fr]">
        <div>
          <Link href="/" className="inline-flex items-center gap-3 text-white" aria-label={`${siteConfig.name}, accueil`}>
            <Image src="/images/logo.png" alt="" width={44} height={44} className="h-11 w-auto" />
            <Wordmark className="text-xl" />
          </Link>
          <p className="mt-5 max-w-xs text-sm leading-relaxed">{siteConfig.description}</p>
          <StoreButtons className="mt-6 flex flex-col gap-3 sm:flex-row md:flex-col" />
        </div>

        <nav aria-label="Pied de page">
          <h2 className="text-sm font-semibold text-white">Navigation</h2>
          <ul className="mt-4 space-y-2.5 text-sm">
            {siteConfig.nav.map((item) => <li key={item.href}><Link href={item.href} className="hover:text-white">{item.label}</Link></li>)}
          </ul>
        </nav>

        <div>
          <h2 className="text-sm font-semibold text-white">Nous contacter</h2>
          <ul className="mt-4 space-y-3 text-sm">
            <li className="flex gap-3"><MapPin className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden /><a href={mapsHref} target="_blank" rel="noopener noreferrer" className="hover:text-white">{contact.street}, {contact.city}<br />{contact.postalBox}, {contact.country}</a></li>
            <li className="flex gap-3"><Phone className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden /><a href={phoneHref} className="hover:text-white">{contact.phone}</a></li>
            <li className="flex gap-3"><MessageCircle className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden /><a href={whatsappHref} target="_blank" rel="noopener noreferrer" className="hover:text-white">WhatsApp {siteConfig.whatsapp.display}</a></li>
            <li className="flex gap-3"><Mail className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-hidden /><a href={`mailto:${contact.email}`} className="hover:text-white">{contact.email}</a></li>
          </ul>
        </div>

        <div>
          <h2 className="text-sm font-semibold text-white">Horaires</h2>
          <dl className="mt-4 space-y-2.5 text-sm">
            {siteConfig.hours.map((entry) => (
              <div key={entry.label} className="flex justify-between gap-4"><dt>{entry.label}</dt><dd className="tabular-nums text-white">{entry.text}</dd></div>
            ))}
          </dl>
        </div>
      </Container>

      <Container><RoadLine className="opacity-60" driveSeconds={24} /></Container>
      <Container className="flex flex-col gap-3 py-6 text-xs text-slate-400 sm:flex-row sm:items-center sm:justify-between">
        <p>&copy; {new Date().getFullYear()} {siteConfig.legalName}. Tous droits réservés.</p>
        <nav aria-label="Informations légales"><ul className="flex flex-wrap gap-x-5 gap-y-2">{legalLinks.map((link) => <li key={link.href}><Link href={link.href} className="hover:text-white">{link.label}</Link></li>)}</ul></nav>
      </Container>
    </footer>
  );
}
