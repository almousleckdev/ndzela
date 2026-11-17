"use client";

import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { CloseContractDialog } from "@/features/drivers/components/close-contract-dialog";
import { ChangeContactDialog } from "@/features/drivers/components/change-contact-dialog";
import { DeleteDriversDialog } from "@/features/drivers/components/delete-drivers-dialog";
import { TemporaryPasswordDialog } from "@/features/drivers/components/temporary-password-dialog";
import { SuspendDialog } from "@/features/drivers/components/suspend-dialog";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver } from "@/features/drivers/types";

export type DriverActionKind = "suspend" | "block" | "deactivate" | "reactivate" | "unlock-login" | "change-contact" | "reset-password" | "remove-vehicle" | "partial-payment" | "delete";
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
  if (can(PERMISSIONS.DRIVERS_CREDENTIALS)) {
    items.push({ kind: "change-contact", label: "Modifier l'email ou le téléphone" });
    items.push({ kind: "reset-password", label: "Nouveau mot de passe" });
  }
  if (can(PERMISSIONS.DRIVERS_UPDATE) && can(PERMISSIONS.CONTRACTS_TERMINATE) && driver.vehicle) items.push({ kind: "remove-vehicle", label: "Clôturer le contrat" });
  if (can(PERMISSIONS.DRIVERS_PARTIAL_PAYMENT) && (driver.partialPaymentAllowed || driver.arrears.unpaidDays >= 3)) {
    items.push({ kind: "partial-payment", label: driver.partialPaymentAllowed ? "Retirer le paiement partiel" : "Autoriser le paiement partiel" });
  }
  if (can(PERMISSIONS.DRIVERS_DELETE) || can(PERMISSIONS.DRIVERS_PURGE)) items.push({ kind: "delete", label: "Supprimer", destructive: true });
  return items;
}

const REASONED = {
  deactivate: { title: "Mettre ce chauffeur inactif ?", text: "Il quitte ou arrête le travail : son véhicule redevient disponible et son contrat en cours est résilié.", button: "Mettre inactif", success: "Chauffeur inactif.", run: driversService.deactivate, destructive: false },
  block: { title: "Bloquer ce chauffeur ?", text: "Le blocage est durable : seul un administrateur pourra le lever. Son véhicule redevient disponible et son contrat en cours est résilié.", button: "Bloquer", success: "Chauffeur bloqué.", run: driversService.block, destructive: true },
  "remove-vehicle": { success: "Contrat clôturé.", run: driversService.removeVehicle },
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
  const { can } = usePermissions();
  if (!target) return null;
  const { kind, driver } = target;
  const name = `${driver.personal.lastName} ${driver.personal.firstName}`;

  if (kind === "suspend") return <SuspendDialog driver={driver} name={name} onClose={onClose} onDone={() => onDone(kind)} />;

  if (kind === "remove-vehicle") {
    return (
      <CloseContractDialog driverId={driver.id} driverName={name} mayCollect={can(PERMISSIONS.PAYMENTS_CREATE)} onClose={onClose}
        onConfirm={async (reason) => {
          await REASONED["remove-vehicle"].run(driver.id, reason);
          addToast({ title: "Succès", description: REASONED["remove-vehicle"].success, type: "success" });
          onDone(kind);
        }} />
    );
  }

  if (kind === "partial-payment") {
    const allowed = !driver.partialPaymentAllowed;
    return (
      <ConfirmDialog isOpen onClose={onClose} title={allowed ? "Autoriser le paiement partiel ?" : "Retirer le paiement partiel ?"}
        description={allowed ? `${name} pourra payer un ou deux jours seulement dans l'application, même avec 3 jours ou plus en retard. L'autorisation s'arrête quand il n'a plus de retard.` : `${name} devra de nouveau tout payer d'un coup.`}
        confirmText={allowed ? "Autoriser" : "Retirer"} cancelText="Annuler"
        onConfirm={async () => {
          try {
            await driversService.allowPartialPayment(driver.id, allowed);
            addToast({ title: "Succès", description: allowed ? "Paiement partiel autorisé." : "Paiement partiel retiré.", type: "success" });
            onDone(kind);
          } catch (error) {
            addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
            onClose();
          }
        }} />
    );
  }

  if (kind === "block" || kind === "deactivate") {
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

  // These two keep their dialog open when the server refuses, so the admin can correct and try again.
  const attempt = async (run: () => Promise<unknown>, success: string) => {
    try {
      await run();
      addToast({ title: "Succès", description: success, type: "success" });
      onDone(kind);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
    }
  };
  if (kind === "change-contact") {
    return <ChangeContactDialog driverName={name} currentEmail={driver.personal.email} currentPhone={driver.personal.phonePrimary} phoneCode={driver.country?.phoneCode ?? null} onClose={onClose}
      onConfirm={(change) => attempt(() => driversService.changeContact(driver.id, change), "Coordonnées mises à jour.")} />;
  }
  if (kind === "reset-password") {
    return <TemporaryPasswordDialog driverName={name} onClose={onClose} onConfirm={(password) => attempt(() => driversService.resetPassword(driver.id, password), "Mot de passe provisoire enregistré.")} />;
  }

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
