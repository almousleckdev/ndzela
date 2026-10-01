"use client";

import { useState } from "react";
import { Pencil, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { chargingService } from "@/features/charging/services/charging.service";
import type { Station } from "@/features/charging/types";

/** The managed list of charging stations: add, rename, delete (refused while a station has recharges). */
export function StationsDialog({ onClose }: { onClose: () => void }) {
  const { addToast } = useToast();
  const stations = useFetch(chargingService.stations);
  const [name, setName] = useState("");
  const [editing, setEditing] = useState<Station | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const run = async (action: () => Promise<unknown>, success: string) => {
    setIsSaving(true);
    try {
      await action();
      addToast({ title: "Succès", description: success, type: "success" });
      setName("");
      setEditing(null);
      stations.reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
    } finally {
      setIsSaving(false);
    }
  };

  const submit = () => {
    if (!name.trim()) return;
    return editing ? run(() => chargingService.updateStation(editing.id, name.trim()), "Station renommée.") : run(() => chargingService.createStation(name.trim()), "Station ajoutée.");
  };

  return (
    <Dialog isOpen onClose={onClose} title="Stations de recharge" description="La liste proposée lors de l'enregistrement d'une recharge.">
      <div className="space-y-4">
        <div className="flex gap-2">
          <Input aria-label="Nom de la station" placeholder="Nom de la station" value={name} maxLength={100} onChange={(event) => setName(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") submit(); }} />
          <Button onClick={submit} isLoading={isSaving} disabled={!name.trim()}>{editing ? "Renommer" : "Ajouter"}</Button>
          {editing && <Button variant="outline" onClick={() => { setEditing(null); setName(""); }}>Annuler</Button>}
        </div>
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {(stations.data ?? []).length === 0 && <li className="px-3 py-3 text-sm text-slate-500">Aucune station.</li>}
          {(stations.data ?? []).map((station) => (
            <li key={station.id} className="flex items-center justify-between gap-2 px-3 py-2 text-sm">
              <span>{station.name}</span>
              <span>
                <Button variant="ghost" size="icon" aria-label={`Renommer ${station.name}`} onClick={() => { setEditing(station); setName(station.name); }}><Pencil className="h-4 w-4" /></Button>
                <Button variant="ghost" size="icon" aria-label={`Supprimer ${station.name}`} onClick={() => run(() => chargingService.removeStation(station.id), "Station supprimée.")}><Trash2 className="h-4 w-4 text-red-600" /></Button>
              </span>
            </li>
          ))}
        </ul>
        <div className="flex justify-end border-t border-slate-200 pt-4"><Button variant="outline" onClick={onClose}>Fermer</Button></div>
      </div>
    </Dialog>
  );
}
