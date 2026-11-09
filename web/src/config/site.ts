export interface OpeningDay {
  // Days of the week the hours apply to, 0 = Sunday.
  days: readonly number[];
  label: string;
  // Opening and closing time in minutes from midnight; null when closed.
  hours: { from: number; to: number } | null;
  text: string;
}

// Facts about the company shown across the site
export const siteConfig = {
  name: "Ndzela",
  legalName: "Ndzela SARL",
  tagline: "Mobilité électrique",
  description: "Ndzela loue des voitures électriques aux chauffeurs VTC de Libreville et exploite sa propre station de recharge, ouverte à tous avec une application.",
  url: process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000",
  locale: "fr_FR",
  timezone: "Africa/Libreville",
  contact: {
    email: "ndzelagabon@gmail.com",
    phone: "+241 62 82 18 55",
    street: "Quartier Sablière",
    postalBox: "BP 10251",
    city: "Libreville",
    country: "Gabon",
  },
  whatsapp: { display: "+241 02 82 18 55", number: "24102821855", message: "Bonjour Ndzela, j'aimerais avoir des informations." },
  hours: [
    { days: [1, 2, 3, 4, 5], label: "Lundi au vendredi", hours: { from: 8 * 60 + 30, to: 17 * 60 }, text: "8h30 - 17h00" },
    { days: [6], label: "Samedi", hours: { from: 9 * 60 + 30, to: 12 * 60 + 30 }, text: "9h30 - 12h30" },
    { days: [0], label: "Dimanche", hours: null, text: "Fermé" },
  ] satisfies OpeningDay[],
  // Store listings of the driver app; null until the app is published, the buttons then say "Bientôt".
  app: { appStore: null as string | null, googlePlay: "https://play.google.com/store/apps/details?id=com.ndzela.rider" as string | null },
  // Store listings of the charging app, same rule as `app`.
  chargingApp: { appStore: null as string | null, googlePlay: null as string | null },
  nav: [
    { label: "Accueil", href: "/" },
    { label: "Location", href: "/#location" },
    { label: "Recharge", href: "/#recharge" },
    { label: "Application", href: "/#application" },
    { label: "Contact", href: "/contact" },
  ],
} as const;

export const legalLinks = [
  { label: "Confidentialité", href: "/confidentialite" },
  { label: "Conditions d'utilisation", href: "/conditions" },
  { label: "Suppression de compte", href: "/suppression-compte" },
] as const;

export const phoneHref = `tel:${siteConfig.contact.phone.replace(/\s/g, "")}`;
export const whatsappHref = `https://wa.me/${siteConfig.whatsapp.number}?text=${encodeURIComponent(siteConfig.whatsapp.message)}`;
export const mapsHref = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${siteConfig.contact.street}, ${siteConfig.contact.city}, ${siteConfig.contact.country}`)}`;
