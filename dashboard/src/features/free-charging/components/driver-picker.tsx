"use client";

import { useState } from "react";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { useAsync } from "@/hooks/use-async";
import { useDebounced } from "@/hooks/use-debounced";
import { driversService } from "@/features/drivers/services/drivers.service";

interface DriverPickerProps {
  value: string;
  onChange: (driverId: string) => void;
  error?: string;
}

interface Option {
  value: string;
  label: string;
}

export function DriverPicker({ value, onChange, error }: DriverPickerProps) {
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const drivers = useAsync(() => driversService.list({ status: "ALL", scope: "CURRENT", q, page: 0, size: 20 }), [q]);
  const [chosen, setChosen] = useState<Option | null>(null);
  const found = (drivers.data?.items ?? []).map((driver) => ({ value: driver.id, label: `${driver.personal.lastName} ${driver.personal.firstName}` }));
  const kept = chosen && chosen.value === value && !found.some((option) => option.value === value) ? [chosen] : [];
  const options = [...kept, ...found];

  const choose = (driverId: string) => {
    setChosen(options.find((option) => option.value === driverId) ?? null);
    onChange(driverId);
  };

  return (
    <div className="space-y-2">
      <FormField label="Rechercher un chauffeur" value={search} onChange={(event) => setSearch(event.target.value)} autoComplete="off" />
      <SelectField label="Chauffeur *" value={value} onChange={(event) => choose(event.target.value)} options={options} placeholder="Choisir un chauffeur" error={error} />
    </div>
  );
}
