import type { Agreement } from "@/features/free-charging/types";

const MIN_PHONE_DIGITS = 3;

function digitsOf(text: string): string {
  return text.replace(/\D/g, "").replace(/^0+/, "");
}

function matches(agreement: Agreement, words: string[], digits: string): boolean {
  const haystack = [agreement.driverName, agreement.email, agreement.fleetNumber, agreement.plate, agreement.phone].filter(Boolean).join(" ").toLowerCase();
  if (words.every((word) => haystack.includes(word))) return true;
  return digits.length >= MIN_PHONE_DIGITS && digitsOf(agreement.phone ?? "").includes(digits);
}

// The list search: name, e-mail, phone (any spacing or prefix), fleet number or plate.
export function filterAgreements(items: Agreement[], text: string): Agreement[] {
  const words = text.toLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return items;
  const digits = /^[\d+()\s.-]+$/.test(text.trim()) ? digitsOf(text) : "";
  return items.filter((agreement) => matches(agreement, words, digits));
}
