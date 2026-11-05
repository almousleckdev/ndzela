"use client";

import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { DOCUMENT_TYPES, IMAGE_TYPES, fileProblem } from "@/components/ui/file-field";
import { useToast } from "@/hooks/use-toast";
import { apiBlob } from "@/lib/api/api-client";
import { getErrorMessage } from "@/lib/errors";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver, DriverFileKind } from "@/features/drivers/types";

const FILES: { kind: DriverFileKind; label: string; has: (driver: Driver) => boolean; image: boolean }[] = [
  { kind: "photo", label: "Photo de profil", has: (d) => d.files.photo, image: true },
  { kind: "licence", label: "Permis de conduire", has: (d) => d.files.licence, image: false },
  { kind: "id", label: "Pièce d'identité", has: (d) => d.files.id, image: false },
  { kind: "address-proof", label: "Casier judiciaire", has: (d) => d.files.addressProof, image: false },
  { kind: "residence-certificate", label: "Certificat de résidence", has: (d) => d.files.residenceCertificate, image: false },
];

// The photo and documents of a driver
export function DriverFilesCard({ driver, canEdit, onChanged }: { driver: Driver; canEdit: boolean; onChanged: () => void }) {
  const { addToast } = useToast();
  const [busy, setBusy] = useState<DriverFileKind | null>(null);
  const inputs = useRef<Partial<Record<DriverFileKind, HTMLInputElement | null>>>({});

  const fail = (error: unknown) => addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });

  const view = async (kind: DriverFileKind) => {
    const tab = window.open("", "_blank");
    try {
      const blob = await apiBlob(driversService.filePath(driver.id, kind));
      if (tab) tab.location.href = URL.createObjectURL(blob);
    } catch (error) {
      tab?.close();
      fail(error);
    }
  };

  const run = async (kind: DriverFileKind, request: () => Promise<unknown>, success: string) => {
    setBusy(kind);
    try {
      await request();
      addToast({ title: "Succès", description: success, type: "success" });
      onChanged();
    } catch (error) {
      fail(error);
    } finally {
      setBusy(null);
    }
  };

  const replace = (kind: DriverFileKind, file: File | undefined, image: boolean) => {
    if (!file) return;
    const problem = fileProblem(file, image ? IMAGE_TYPES : DOCUMENT_TYPES);
    if (problem) return addToast({ title: "Erreur", description: problem, type: "error" });
    return run(kind, () => driversService.replaceFile(driver.id, kind, file), "Fichier enregistré.");
  };

  return (
    <Card>
      <CardTitle>Photo et documents</CardTitle>
      <ul className="divide-y divide-slate-100">
        {FILES.map(({ kind, label, has, image }) => (
          <li key={kind} className="flex flex-wrap items-center justify-between gap-2 py-2.5 text-sm">
            <div>
              <p className="font-medium text-slate-900">{label}</p>
              <p className="text-xs text-slate-500">{has(driver) ? "Fourni" : "Non fourni"}</p>
            </div>
            <div className="flex items-center gap-2">
              {has(driver) && <Button variant="outline" size="sm" onClick={() => view(kind)}>Voir</Button>}
              {canEdit && (
                <>
                  <input ref={(node) => { inputs.current[kind] = node; }} type="file" className="hidden" aria-label={`${label} : choisir un fichier`}
                    accept={(image ? IMAGE_TYPES : DOCUMENT_TYPES).join(",")} onChange={(event) => { replace(kind, event.target.files?.[0], image); event.target.value = ""; }} />
                  <Button variant="outline" size="sm" isLoading={busy === kind} onClick={() => inputs.current[kind]?.click()}>{has(driver) ? "Remplacer" : "Ajouter"}</Button>
                  {has(driver) && kind !== "photo" && (
                    <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" disabled={busy === kind}
                      onClick={() => run(kind, () => driversService.removeFile(driver.id, kind), "Fichier retiré.")}>Retirer</Button>
                  )}
                </>
              )}
            </div>
          </li>
        ))}
      </ul>
    </Card>
  );
}
