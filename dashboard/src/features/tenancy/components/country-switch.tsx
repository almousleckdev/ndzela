"use client";

import { useEffect } from "react";
import { Globe } from "lucide-react";
import { Select } from "@/components/ui/select";
import { useFetch } from "@/hooks/use-fetch";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { CountryFlag } from "@/features/tenancy/components/country-flag";
import { countriesService } from "@/features/tenancy/services/countries.service";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";

const ALL = "";

// Header
export function CountrySwitch() {
  const user = useAuthStore((state) => state.user);
  if (!user) return null;

  if (user.country) {
    return (
      <span className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1.5 text-sm font-medium text-slate-700">
        <CountryFlag code={user.country.code} />
        {user.country.name}
      </span>
    );
  }
  return <CountrySelector />;
}

function CountrySelector() {
  const { data: countries } = useFetch(countriesService.list);
  const selectedCode = useCountrySelectionStore((state) => state.selectedCode);
  const select = useCountrySelectionStore((state) => state.select);

  // A remembered country that no longer exists or is inactive would make every request fail: fall back to "all".
  useEffect(() => {
    if (countries && selectedCode && !countries.some((country) => country.code === selectedCode)) select(null);
  }, [countries, selectedCode, select]);

  return (
    <div className="flex items-center gap-2">
      {selectedCode ? <CountryFlag code={selectedCode} /> : <Globe className="h-4 w-4 text-slate-500" aria-hidden="true" />}
      <Select
        aria-label="Pays affiché"
        className="w-44"
        value={selectedCode ?? ALL}
        onChange={(event) => select(event.target.value === ALL ? null : event.target.value)}
      >
        <option value={ALL}>Tous les pays</option>
        {countries?.map((country) => (
          <option key={country.id} value={country.code}>{country.name}</option>
        ))}
        {selectedCode && !countries?.some((country) => country.code === selectedCode) && <option value={selectedCode}>{selectedCode}</option>}
      </Select>
    </div>
  );
}
