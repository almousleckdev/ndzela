"use client";

import { useId } from "react";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { VEHICLE_COLORS, colorName, isHexColor } from "@/lib/colors";

/** A colour shown as a dot and its name; values that are not hex (older vehicles) show as text. */
export function ColorLabel({ value }: { value: string }) {
  return (
    <span className="inline-flex items-center gap-1.5">
      {isHexColor(value) && <span aria-hidden className="h-3 w-3 rounded-full border border-slate-300" style={{ backgroundColor: value }} />}
      {colorName(value)}
    </span>
  );
}

interface ColorPickerProps {
  label: string;
  value: string;
  error?: string;
  onChange: (hex: string) => void;
}

/** Swatches for the usual car colours plus a free colour picker. */
export function ColorPicker({ label, value, error, onChange }: ColorPickerProps) {
  const id = useId();
  const current = isHexColor(value) ? value : "#ffffff";

  return (
    <div>
      <Label htmlFor={id}>{label}</Label>
      <div className="flex flex-wrap items-center gap-2 rounded-lg border border-slate-300 bg-white p-2">
        {VEHICLE_COLORS.map((color) => (
          <button key={color.hex} type="button" title={color.name} aria-label={color.name} aria-pressed={value.toLowerCase() === color.hex.toLowerCase()}
            onClick={() => onChange(color.hex)} style={{ backgroundColor: color.hex }}
            className={cn("h-6 w-6 rounded-full border border-slate-300 transition-transform hover:scale-110", value.toLowerCase() === color.hex.toLowerCase() && "ring-2 ring-primary ring-offset-1")} />
        ))}
        <input id={id} type="color" value={current} onChange={(event) => onChange(event.target.value)} aria-label="Autre couleur" className="h-7 w-9 cursor-pointer rounded border border-slate-300 bg-white p-0.5" />
        <span className="ml-auto text-xs text-slate-500">{value ? colorName(value) : "Aucune couleur"}</span>
      </div>
      {error && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
    </div>
  );
}
