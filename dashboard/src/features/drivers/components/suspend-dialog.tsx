"use client";

import { useState } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useToast } from "@/hooks/use-toast";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

// A suspension keeps the car unless staff choose to free it.
export function SuspendDialog({ driver, name, onClose, onDone }: { driver: Driver; name: string; onClose: () => void; onDone: () => void }) {
  const { addToast } = useToast();
  const [release, setRelease] = useState(false);
  return (
    <ReasonDialog title="Suspendre ce chauffeur ?" description={`${name}. Il ne pourra plus utiliser l'application. Vous pourrez le réactiver.`} confirmText="Suspendre" onClose={onClose}
      onConfirm={async (reason) => {
        await driversService.suspend(driver.id, reason, release);
        addToast({ title: "Succès", description: "Chauffeur suspendu.", type: "success" });
        onDone();
      }}>
      {driver.vehicle && (
        <Checkbox checked={release} onChange={(event) => setRelease(event.target.checked)} label="Libérer le véhicule"
          description="Le véhicule redevient disponible et le contrat en cours est résilié." />
      )}
    </ReasonDialog>
  );
}
