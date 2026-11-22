"use client";

import { useMemo, useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { AgreementFormDialog } from "@/features/free-charging/components/agreement-form-dialog";
import { agreementColumns, type AgreementAction } from "@/features/free-charging/components/agreement-columns";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { Agreement } from "@/features/free-charging/types";

const DONE: Record<"suspend" | "resume" | "revoke", string> = {
  suspend: "Accord suspendu.",
  resume: "Accord repris.",
  revoke: "Accord retiré. L'historique est conservé.",
};

interface AgreementsTabProps {
  windowText?: string;
}

export function AgreementsTab({ windowText = "pendant le créneau gratuit" }: AgreementsTabProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const canManage = can(PERMISSIONS.FREE_CHARGING_MANAGE);
  const list = useAsync(freeChargingService.list, []);
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<Agreement | null>(null);
  const [revoking, setRevoking] = useState<Agreement | null>(null);
  const [revokePending, setRevokePending] = useState(false);
  const [revokeError, setRevokeError] = useState<string | null>(null);

  const run = async (kind: keyof typeof DONE, agreement: Agreement): Promise<boolean> => {
    try {
      await freeChargingService[kind](agreement.id);
      addToast({ title: "Succès", description: DONE[kind], type: "success" });
      list.reload();
      return true;
    } catch (error) {
      const message = getErrorMessage(error, "L'action a échoué.");
      addToast({ title: "Erreur", description: message, type: "error" });
      if (kind === "revoke") setRevokeError(message);
      return false;
    }
  };

  const onAction = (action: AgreementAction, agreement: Agreement) => {
    if (action === "edit") setEditing(agreement);
    else if (action === "revoke") { setRevokeError(null); setRevoking(agreement); }
    else void run(action, agreement);
  };

  const confirmRevoke = async () => {
    if (!revoking) return;
    setRevokePending(true);
    setRevokeError(null);
    const done = await run("revoke", revoking);
    setRevokePending(false);
    if (done) setRevoking(null);
  };

  const save = async (driverId: string, input: Parameters<typeof freeChargingService.create>[1]) => {
    if (editing) await freeChargingService.update(editing.id, input);
    else await freeChargingService.create(driverId, input);
    addToast({ title: "Succès", description: editing ? "Accord modifié." : "Chauffeur ajouté.", type: "success" });
    setAdding(false);
    setEditing(null);
    list.reload();
  };

  const columns = useMemo(() => agreementColumns(canManage, onAction), [canManage]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Panel>
      <Toolbar start={<p className="text-sm text-muted">{`Une recharge gratuite par jour de gratuité, ${windowText}, sans jour impayé.`}</p>}
        end={canManage && <Button onClick={() => setAdding(true)}><Plus className="mr-2 h-4 w-4" /> Ajouter un chauffeur</Button>} />
      <DataTable columns={columns} data={list.data} error={list.error} onRetry={list.reload} getRowId={(row) => row.id}
        empty={<EmptyState title="Aucun chauffeur" description="Aucun accord de gratuité pour le moment." />} />
      {(adding || editing) && <AgreementFormDialog agreement={editing ?? undefined} windowText={windowText} onClose={() => { setAdding(false); setEditing(null); }} onSubmit={save} />}
      {revoking && (
        <ConfirmDialog isOpen onClose={() => setRevoking(null)} onConfirm={confirmRevoke} isLoading={revokePending} destructive
          title="Retirer cet accord ?" description={`${revoking.driverName} ne rechargera plus gratuitement le dimanche. L'historique est conservé.${revokeError ? ` ${revokeError}` : ""}`}
          confirmText="Retirer l'accord" cancelText="Annuler" />
      )}
    </Panel>
  );
}
