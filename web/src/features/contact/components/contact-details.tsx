import { Mail, MapPin, MessageCircle, Phone, type LucideIcon } from "lucide-react";
import { mapsHref, phoneHref, siteConfig, whatsappHref } from "@/config/site";

const { contact } = siteConfig;

const ITEMS: { icon: LucideIcon; title: string; lines: string[]; href: string; action: string; external?: boolean }[] = [
  { icon: Phone, title: "Téléphone", lines: [contact.phone], href: phoneHref, action: "Appeler" },
  { icon: MessageCircle, title: "WhatsApp", lines: [siteConfig.whatsapp.display], href: whatsappHref, action: "Discuter sur WhatsApp", external: true },
  { icon: Mail, title: "E-mail", lines: [contact.email], href: `mailto:${contact.email}`, action: "Écrire" },
  { icon: MapPin, title: "Adresse", lines: [`${contact.street}, ${contact.city}`, `${contact.postalBox}, ${contact.country}`], href: mapsHref, action: "Voir sur la carte", external: true },
];

export function ContactDetails() {
  return (
    <ul className="grid gap-4">
      {ITEMS.map(({ icon: Icon, title, lines, href, action, external }) => (
        <li key={title} className="flex items-start gap-5 rounded-3xl border border-line p-6">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-sky text-action"><Icon className="h-6 w-6" aria-hidden /></span>
          <div className="min-w-0 flex-1">
            <h2 className="font-display text-lg font-bold">{title}</h2>
            {lines.map((line) => <p key={line} className="mt-1 break-words text-muted">{line}</p>)}
            <a href={href} {...(external ? { target: "_blank", rel: "noopener noreferrer" } : {})} className="mt-3 inline-block font-semibold text-action hover:text-action-hover">{action}</a>
          </div>
        </li>
      ))}
    </ul>
  );
}
