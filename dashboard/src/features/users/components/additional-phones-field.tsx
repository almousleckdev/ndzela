import { Plus, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

const MAX_EXTRA_PHONES = 5;

// A list of extra phone numbers the user can add to and remove from.
export function AdditionalPhonesField({ phones, onChange }: { phones: string[]; onChange: (phones: string[]) => void }) {
  return (
    <div>
      <Label>Autres numéros</Label>
      <div className="space-y-2">
        {phones.map((phone, index) => (
          <div key={index} className="flex gap-2">
            <input
              type="tel"
              className="h-10 flex-1 rounded-lg border border-slate-300 px-3 text-sm"
              value={phone}
              onChange={(event) => onChange(phones.map((p, i) => (i === index ? event.target.value : p)))}
              placeholder="Numéro supplémentaire"
            />
            <Button type="button" variant="outline" size="icon" aria-label="Retirer ce numéro" onClick={() => onChange(phones.filter((_, i) => i !== index))}>
              <X className="h-4 w-4" />
            </Button>
          </div>
        ))}
        {phones.length < MAX_EXTRA_PHONES && (
          <Button type="button" variant="outline" onClick={() => onChange([...phones, ""])}>
            <Plus className="mr-2 h-4 w-4" /> Ajouter un numéro
          </Button>
        )}
      </div>
    </div>
  );
}
