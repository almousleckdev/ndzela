const ZONE = "Africa/Libreville";

export const money = (amount: number, currency: string): string =>
  `${new Intl.NumberFormat("fr-FR").format(amount).replace(/ /g, " ")} ${currency === "XAF" ? "FCFA" : currency}`;

export const dateTime = (iso: string): string =>
  new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeStyle: "short", timeZone: ZONE }).format(new Date(iso));

export const dayLabel = (day: string): string =>
  new Intl.DateTimeFormat("fr-FR", { weekday: "long", day: "numeric", month: "long", year: "numeric", timeZone: "UTC" }).format(new Date(`${day}T00:00:00Z`));
