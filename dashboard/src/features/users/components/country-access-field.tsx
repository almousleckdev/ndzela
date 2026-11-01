import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { Country } from "@/features/auth/types";

interface CountryAccessFieldProps {
  countries: Country[] | null;
  isGlobal: boolean;
  onGlobalChange: () => void;
  countryIds: string[];
  onToggleCountry: (id: string) => void;
  error?: string;
}

// Global access, or the countries a user works in; only a global administrator sees it.
export function CountryAccessField({ countries, isGlobal, onGlobalChange, countryIds, onToggleCountry, error }: CountryAccessFieldProps) {
  return (
    <div>
      <Checkbox label="Utilisateur global" description="Voit et gère tous les pays, pas seulement ceux cochés ci-dessous." checked={isGlobal} onChange={onGlobalChange} />
      {!isGlobal && (
        <fieldset className="mt-2">
          <Label asLegend>Pays</Label>
          <div className="max-h-40 overflow-y-auto rounded-lg border border-slate-200 p-1">
            {countries?.map((country) => (
              <Checkbox key={country.id} label={country.name} checked={countryIds.includes(country.id)} onChange={() => onToggleCountry(country.id)} />
            ))}
          </div>
          {error && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
        </fieldset>
      )}
    </div>
  );
}
