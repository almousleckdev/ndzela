"use client";

import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { DeleteDriversDialog } from "@/features/drivers/components/delete-drivers-dialog";
import { SuspendDialog } from "@/features/drivers/components/suspend-dialog";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

export type DriverActionKind = "suspend" | "block" | "deactivate" | "reactivate" | "unlock-login" | "remove-vehicle" | "delete";
export interface DriverAction { kind: DriverActionKind; driver: Driver }

// The state-changing actions offered for a driver, according to their state and the viewer's permissions.
export function availableActions(driver: Driver, can: (permission: Permission) => boolean): { kind: DriverActionKind; label: string; destructive?: boolean }[] {
  const items: { kind: DriverActionKind; label: string; destructive?: boolean }[] = [];
  const liftedBy = { ACTIVE: null, SUSPENDED: PERMISSIONS.DRIVERS_SUSPEND, BLOCKED: PERMISSIONS.DRIVERS_BLOCK, INACTIVE: PERMISSIONS.DRIVERS_UPDATE } as const;
  if (can(PERMISSIONS.DRIVERS_SUSPEND) && (driver.status === "ACTIVE" || driver.status === "INACTIVE")) items.push({ kind: "suspend", label: "Suspendre" });
  if (can(PERMISSIONS.DRIVERS_UPDATE) && driver.status === "ACTIVE") items.push({ kind: "deactivate", label: "Mettre inactif" });
  const lifter = liftedBy[driver.status];
  if (lifter && can(lifter)) items.push({ kind: "reactivate", label: "Réactiver" });
  if (can(PERMISSIONS.DRIVERS_BLOCK) && driver.status !== "BLOCKED") items.push({ kind: "block", label: "Bloquer", destructive: true });
  if (can(PERMISSIONS.DRIVERS_UNLOCK) && driver.loginLocked) items.push({ kind: "unlock-login", label: "Débloquer la connexion" });
  if (can(PERMISSIONS.DRIVERS_UPDATE) && driver.vehicle) items.push({ kind: "remove-vehicle", label: "Retirer le véhicule" });
  if (can(PERMISSIONS.DRIVERS_DELETE) || can(PERMISSIONS.DRIVERS_PURGE)) items.push({ kind: "delete", label: "Supprimer", destructive: true });
  return items;
}

const REASONED = {
  deactivate: { title: "Mettre ce chauffeur inactif ?", text: "Il quitte ou arrête le travail : son véhicule redevient disponible et son contrat en cours est résilié.", button: "Mettre inactif", success: "Chauffeur inactif.", run: driversService.deactivate, destructive: false },
  block: { title: "Bloquer ce chauffeur ?", text: "Le blocage est durable : seul un administrateur pourra le lever. Son véhicule redevient disponible et son contrat en cours est résilié.", button: "Bloquer", success: "Chauffeur bloqué.", run: driversService.block, destructive: true },
  "remove-vehicle": { title: "Retirer le véhicule ?", text: "Le véhicule redeviendra disponible et le contrat en cours sera résilié. L'historique est conservé.", button: "Retirer", success: "Véhicule retiré.", run: driversService.removeVehicle, destructive: false },
} as const;

interface DriverActionDialogsProps {
  target: DriverAction | null;
  onClose: () => void;
  // Called after the action succeeded, so the caller can reload or leave the page.
  onDone: (kind: DriverActionKind) => void;
}

// The dialogs behind the driver actions
export function DriverActionDialogs({ target, onClose, onDone }: DriverActionDialogsProps) {
  const { addToast } = useToast();
  if (!target) return null;
  const { kind, driver } = target;
  const name = `${driver.personal.lastName} ${driver.personal.firstName}`;

  if (kind === "suspend") return <SuspendDialog driver={driver} name={name} onClose={onClose} onDone={() => onDone(kind)} />;

  if (kind === "block" || kind === "deactivate" || kind === "remove-vehicle") {
    const copy = REASONED[kind];
    return (
      <ReasonDialog title={copy.title} description={`${name}. ${copy.text}`} confirmText={copy.button} destructive={copy.destructive} onClose={onClose}
        onConfirm={async (reason) => {
          await copy.run(driver.id, reason);
          addToast({ title: "Succès", description: copy.success, type: "success" });
          onDone(kind);
        }} />
    );
  }

  if (kind === "delete") return <DeleteDriversDialog drivers={[driver]} onClose={onClose} onDone={() => onDone(kind)} />;

  const isUnlock = kind === "unlock-login";
  const confirm = async () => {
    try {
      if (isUnlock) await driversService.unlockLogin(driver.id);
      else await driversService.reactivate(driver.id);
      addToast({ title: "Succès", description: isUnlock ? "Connexion débloquée." : "Chauffeur réactivé.", type: "success" });
      onDone(kind);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
      onClose();
    }
  };
  return (
    <ConfirmDialog isOpen onClose={onClose} onConfirm={confirm}
      title={isUnlock ? "Débloquer la connexion de ce chauffeur ?" : "Réactiver ce chauffeur ?"}
      description={
        isUnlock
          ? `${name} a été bloqué après plusieurs tentatives de connexion échouées. Il pourra se reconnecter immédiatement, sans attendre la fin du blocage.`
          : `${name} redeviendra actif.`
      }
      confirmText={isUnlock ? "Débloquer" : "Réactiver"} cancelText="Annuler" />
  );
}
