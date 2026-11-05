"use client";

import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { Tabs } from "@/components/ui/tabs";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { chargingService } from "@/features/charging/services/charging.service";
import type { IdTagRow, IdTagStatus } from "@/features/charging/types";

const TABS = [{ value: "SUGGESTED" as const, label: "À confirmer" }, { value: "LINKED" as const, label: "Liés" }, { value: "UNLINKED" as const, label: "Refusés" }];
const MATCH: Record<string, string> = { "email+phone": "E-mail et téléphone", email: "E-mail seulement", phone: "Téléphone seulement" };

// Which driver is behind each charging account of the Zella app: confirm a suggestion, or unlink.
export function IdTagsDialog({ onClose }: { onClose: () => void }) {
  const { addToast } = useToast();
  const [status, setStatus] = useState<IdTagStatus>("SUGGESTED");
  const [unlinking, setUnlinking] = useState<IdTagRow | null>(null);
  const [busy, setBusy] = useState(false);
  const [found, setFound] = useState<number | null>(null);
  const list = useAsync(() => chargingService.idTags(status), [status]);

  const run = async (action: () => Promise<unknown>, success: string) => {
    setBusy(true);
    try {
      await action();
      addToast({ title: "Succès", description: success, type: "success" });
      list.reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Comptes de recharge" description="Le compte de l'application Zella Charging et le chauffeur NDZela auquel il correspond." className="max-w-3xl">
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <Tabs tabs={TABS} value={status} onChange={setStatus} label="Comptes de recharge" />
          <Button variant="outline" size="sm" isLoading={busy} onClick={() => run(async () => { const r = await chargingService.lookupIdTags(); setFound(r.stored); }, "Recherche terminée.")}>Rechercher</Button>
        </div>
        {found !== null && <p className="text-xs text-slate-500">{found} compte(s) trouvé(s) à la dernière recherche.</p>}
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {list.data?.length === 0 && <li className="px-3 py-3 text-sm text-slate-500">Aucun compte dans cette liste.</li>}
          {(list.data ?? []).map((row) => (
            <li key={row.id} className="flex flex-wrap items-center justify-between gap-3 px-3 py-3 text-sm">
              <div className="min-w-0 space-y-0.5">
                <p className="font-medium text-slate-900">{row.driverName} <span className="font-normal text-slate-500">· {row.driverEmail}</span></p>
                <p className="text-slate-600">Zella : {row.zellaName || "-"} · {row.idTag ? <span className="font-mono text-xs">{row.idTag}</span> : <span className="text-xs">pas encore de recharge</span>}</p>
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
                  <Badge variant={row.matchedOn === "email+phone" ? "success" : "warning"}>{MATCH[row.matchedOn] ?? row.matchedOn}</Badge>
                  {!row.nameMatches && <span className="inline-flex items-center gap-1 text-amber-700"><AlertTriangle className="h-3 w-3" /> Les noms sont différents</span>}
                </div>
              </div>
              <div className="flex gap-2">
                {row.status !== "LINKED" && <Button size="sm" disabled={busy} onClick={() => run(() => chargingService.confirmIdTag(row.id), "Compte lié au chauffeur.")}>{row.status === "SUGGESTED" ? "Confirmer" : "Lier à nouveau"}</Button>}
                {row.status !== "UNLINKED" && <Button size="sm" variant="outline" disabled={busy} onClick={() => setUnlinking(row)}>{row.status === "SUGGESTED" ? "Refuser" : "Délier"}</Button>}
              </div>
            </li>
          ))}
        </ul>
        <div className="flex justify-end border-t border-slate-200 pt-4"><Button variant="outline" onClick={onClose}>Fermer</Button></div>
      </div>
      {unlinking && (
        <ReasonDialog title="Délier ce compte ?" description={`${unlinking.driverName} · ${unlinking.idTag ?? unlinking.customerId}. Ses sessions s'afficheront comme « Public » et le compte ne sera plus lié automatiquement.`} confirmText="Délier" destructive onClose={() => setUnlinking(null)}
          onConfirm={async (reason) => { await chargingService.unlinkIdTag(unlinking.id, reason); setUnlinking(null); addToast({ title: "Succès", description: "Compte délié.", type: "success" }); list.reload(); }} />
      )}
    </Dialog>
  );
}
