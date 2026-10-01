"use client";

import { useState } from "react";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { trainingCentersService } from "@/features/drivers/services/training-centers.service";

/** The managed list of training centres: add, rename, delete. */
export function TrainingCentersDialog({ onClose }: { onClose: () => void }) {
  const { addToast } = useToast();
  const centers = useFetch(trainingCentersService.list);
  const [newName, setNewName] = useState("");
  const [editing, setEditing] = useState<{ id: string; name: string } | null>(null);

  const run = async (request: () => Promise<unknown>, success: string): Promise<boolean> => {
    try {
      await request();
      addToast({ title: "Succès", description: success, type: "success" });
      centers.reload();
      return true;
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
      return false;
    }
  };

  const add = async () => {
    if (!newName.trim()) return;
    if (await run(() => trainingCentersService.create(newName), "Centre ajouté.")) setNewName("");
  };

  const rename = async () => {
    if (!editing || !editing.name.trim()) return;
    const target = editing;
    setEditing(null);
    await run(() => trainingCentersService.update(target.id, target.name), "Centre modifié.");
  };

  return (
    <Dialog isOpen onClose={onClose} title="Centres de formation" description="La liste proposée lors de la création d'un chauffeur.">
      <div className="space-y-4">
        <div className="flex gap-2">
          <Input aria-label="Nom du nouveau centre" placeholder="Nom du centre" value={newName} onChange={(event) => setNewName(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Enter") add(); }} />
          <Button onClick={add}><Plus className="mr-2 h-4 w-4" /> Ajouter</Button>
        </div>
        {!centers.data ? <Skeleton className="h-24 w-full" /> : centers.data.length === 0 ? (
          <p className="text-sm text-slate-500">Aucun centre pour l&apos;instant.</p>
        ) : (
          <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
            {centers.data.map((center) => (
              <li key={center.id} className="flex items-center justify-between gap-3 px-3 py-2">
                {editing?.id === center.id ? (
                  <Input aria-label="Nouveau nom" autoFocus value={editing.name} onChange={(event) => setEditing({ id: center.id, name: event.target.value })}
                    onKeyDown={(event) => { if (event.key === "Enter") rename(); if (event.key === "Escape") setEditing(null); }} onBlur={rename} />
                ) : (
                  <span className="text-sm text-slate-800">{center.name}</span>
                )}
                <div className="flex shrink-0 gap-1">
                  <Button variant="ghost" size="icon" aria-label={`Renommer ${center.name}`} onClick={() => setEditing({ id: center.id, name: center.name })}><Pencil className="h-4 w-4" /></Button>
                  <Button variant="ghost" size="icon" aria-label={`Supprimer ${center.name}`} onClick={() => run(() => trainingCentersService.remove(center.id), "Centre supprimé.")}><Trash2 className="h-4 w-4 text-red-600" /></Button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <div className="flex justify-end border-t border-slate-200 pt-4"><Button variant="outline" onClick={onClose}>Fermer</Button></div>
      </div>
    </Dialog>
  );
}
