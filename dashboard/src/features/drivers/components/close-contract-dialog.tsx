"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useAsync } from "@/hooks/use-async";
import { formatFcfa } from "@/lib/money";
import { RecordPaymentDialog } from "@/features/payments/components/record-payment-dialog";
import { paymentsService } from "@/features/payments/services/payments.service";

interface CloseContractDialogProps {
  driverId: string;
  driverName: string;
  // Holds payments:create, so the late rent can be collected from here with Encaisser.
  mayCollect: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => Promise<void>;
  // Wording: closing a contract by default, archiving a driver who resigned otherwise
  title?: string;
  description?: string;
  confirmText?: string;
}

// Closes a contract: only once no late rent is owed. While something is owed, staff collect it with Encaisser first.
export function CloseContractDialog({ driverId, driverName, mayCollect, onClose, onConfirm, title = "Clôturer le contrat ?", description, confirmText = "Clôturer" }: CloseContractDialogProps) {
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  const [paying, setPaying] = useState(false);
  const owed = account.data ? account.data.unpaid > 0 || account.data.overdueAmount > 0 : false;

  if (account.isLoading && !account.data) return <Dialog isOpen onClose={onClose} title={title}><p className="text-sm text-slate-600">Chargement du compte…</p></Dialog>;

  if (paying && account.data) {
    return <RecordPaymentDialog driverId={driverId} driverName={driverName} overdueAmount={account.data.overdueAmount} dailyRent={account.data.dailyRent} onClose={() => setPaying(false)} onSaved={() => { setPaying(false); account.reload(); }} />;
  }

  if (owed && account.data) {
    return (
      <Dialog isOpen onClose={onClose} title="Des loyers restent à payer" description={`${driverName} doit ${formatFcfa(account.data.overdueAmount)} (${account.data.unpaid} jour${account.data.unpaid > 1 ? "s" : ""} en retard). Rien ne peut être clôturé tant que tout n'est pas payé.`}>
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose}>Fermer</Button>
          {mayCollect && <Button onClick={() => setPaying(true)}>Encaisser</Button>}
        </div>
      </Dialog>
    );
  }

  return (
    <ReasonDialog title={title} description={description ?? `${driverName}. Le contrat en cours est clôturé et le véhicule redevient disponible. L'historique est conservé.`} confirmText={confirmText} onClose={onClose} onConfirm={onConfirm} />
  );
}
