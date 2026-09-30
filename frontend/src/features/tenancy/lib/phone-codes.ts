import type { Country } from "@/features/auth/types";
import type { PhoneCodeOption } from "@/components/ui/phone-field";

/** One option per distinct dialing code; falls back to the user's own country when the list is unavailable. */
export function phoneCodeOptions(countries: Country[] | null, own: Country | null): PhoneCodeOption[] {
  const source = countries && countries.length > 0 ? countries : own ? [own] : [];
  const seen = new Set<string>();
  return source
    .filter((country) => !seen.has(country.phoneCode) && seen.add(country.phoneCode))
    .map((country) => ({ value: country.phoneCode, label: `${country.phoneCode} ${country.name}` }));
}
