"use client";

import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

export type DriverActionKind = "suspend" | "block" | "deactivate" | "reactivate" | "remove-vehicle" | "delete";
export interface DriverAction { kind: DriverActionKind; driver: Driver }

/** The state-changing actions offered for a driver, according to their state and the viewer's permissions. */
export function availableActions(driver: Driver, can: (permission: Permission) => boolean): { kind: DriverActionKind; label: string; destructive?: boolean }[] {
  const items: { kind: DriverActionKind; label: string; destructive?: boolean }[] = [];
  const liftedBy = { ACTIVE: null, SUSPENDED: PERMISSIONS.DRIVERS_SUSPEND, BLOCKED: PERMISSIONS.DRIVERS_BLOCK, INACTIVE: PERMISSIONS.DRIVERS_UPDATE } as const;
  if (can(PERMISSIONS.DRIVERS_SUSPEND) && (driver.status === "ACTIVE" || driver.status === "INACTIVE")) items.push({ kind: "suspend", label: "Suspendre" });
  if (can(PERMISSIONS.DRIVERS_UPDATE) && driver.status === "ACTIVE") items.push({ kind: "deactivate", label: "Mettre inactif" });
  const lifter = liftedBy[driver.status];
  if (lifter && can(lifter)) items.push({ kind: "reactivate", label: "Réactiver" });
  if (can(PERMISSIONS.DRIVERS_BLOCK) && driver.status !== "BLOCKED") items.push({ kind: "block", label: "Bloquer", destructive: true });
  if (can(PERMISSIONS.DRIVERS_UPDATE) && driver.vehicle) items.push({ kind: "remove-vehicle", label: "Retirer le véhicule" });
  if (can(PERMISSIONS.DRIVERS_DELETE)) items.push({ kind: "delete", label: "Supprimer", destructive: true });
  return items;
}

const REASONED = {
  deactivate: { title: "Mettre ce chauffeur inactif ?", text: "Il ne travaille pas pour le moment. Ce n'est pas une sanction.", button: "Mettre inactif", success: "Chauffeur inactif.", run: driversService.deactivate, destructive: false },
  suspend: { title: "Suspendre ce chauffeur ?", text: "Il ne pourra plus utiliser l'application. Vous pourrez le réactiver.", button: "Suspendre", success: "Chauffeur suspendu.", run: driversService.suspend, destructive: false },
  block: { title: "Bloquer ce chauffeur ?", text: "Le blocage est durable : seul un administrateur pourra le lever.", button: "Bloquer", success: "Chauffeur bloqué.", run: driversService.block, destructive: true },
  "remove-vehicle": { title: "Retirer le véhicule ?", text: "Le véhicule redeviendra disponible et le contrat en cours sera résilié. L'historique est conservé.", button: "Retirer", success: "Véhicule retiré.", run: driversService.removeVehicle, destructive: false },
} as const;

interface DriverActionDialogsProps {
  target: DriverAction | null;
  onClose: () => void;
  /** Called after the action succeeded, so the caller can reload or leave the page. */
  onDone: (kind: DriverActionKind) => void;
}

/** The dialogs behind the driver actions: suspend, block and vehicle removal ask for a reason; the others for a confirmation. */
export function DriverActionDialogs({ target, onClose, onDone }: DriverActionDialogsProps) {
  const { addToast } = useToast();
  if (!target) return null;
  const { kind, driver } = target;
  const name = `${driver.personal.lastName} ${driver.personal.firstName}`;

  if (kind === "suspend" || kind === "block" || kind === "deactivate" || kind === "remove-vehicle") {
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

  const isDelete = kind === "delete";
  const confirm = async () => {
    try {
      await (isDelete ? driversService.remove(driver.id) : driversService.reactivate(driver.id));
      addToast({ title: "Succès", description: isDelete ? "Chauffeur supprimé." : "Chauffeur réactivé.", type: "success" });
      onDone(kind);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
      onClose();
    }
  };
  return (
    <ConfirmDialog isOpen onClose={onClose} onConfirm={confirm} destructive={isDelete}
      title={isDelete ? "Supprimer ce chauffeur ?" : "Réactiver ce chauffeur ?"}
      description={isDelete ? `${name} sera supprimé définitivement avec ses contrats, ses arrêts et ses affectations, et son véhicule redeviendra disponible. Impossible si un paiement ou une recharge est enregistré : suspendez-le alors.` : `${name} redeviendra actif.`}
      confirmText={isDelete ? "Supprimer" : "Réactiver"} cancelText="Annuler" />
  );
}
