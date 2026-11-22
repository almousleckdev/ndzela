"use client";

import { useMemo, useState } from "react";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { SearchField } from "@/components/data-table/search-field";
import { Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { AgreementCreateDialog } from "@/features/free-charging/components/agreement-create-dialog";
import { AgreementEditDialog } from "@/features/free-charging/components/agreement-edit-dialog";
import { AgreementStats } from "@/features/free-charging/components/agreement-stats";
import { agreementColumns, type AgreementAction } from "@/features/free-charging/components/agreement-columns";
import { filterAgreements } from "@/features/free-charging/lib/filter-agreements";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { Agreement, AgreementInput } from "@/features/free-charging/types";

const DONE: Record<"suspend" | "resume" | "revoke", string> = {
  suspend: "Accord suspendu.",
  resume: "Accord repris.",
  revoke: "Accord retiré. L'historique est conservé.",
};

interface AgreementsTabProps {
  windowText?: string;
  adding: boolean;
  onAddingClose: () => void;
}

export function AgreementsTab({ windowText, adding, onAddingClose }: AgreementsTabProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const canManage = can(PERMISSIONS.FREE_CHARGING_MANAGE);
  const list = useAsync(freeChargingService.list, []);
  const [text, setText] = useState("");
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

  const saveEdit = async (input: AgreementInput) => {
    if (!editing) return;
    await freeChargingService.update(editing.id, input);
    addToast({ title: "Succès", description: "Accord modifié.", type: "success" });
    setEditing(null);
    list.reload();
  };

  const added = (addedCount: number, removedCount: number) => {
    addToast({ title: "Succès", description: `${addedCount} ajouté(s), ${removedCount} retiré(s).`, type: "success" });
    onAddingClose();
    list.reload();
  };

  const closeCreate = () => {
    onAddingClose();
    list.reload();
  };

  const rows = filterAgreements(list.data ?? [], text);

  const columns = useMemo(() => agreementColumns(canManage, onAction), [canManage]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="space-y-4">
      {list.data && list.data.length > 0 && <AgreementStats items={list.data} />}
      <Panel>
        <Toolbar start={<SearchField value={text} onChange={setText} label="Rechercher dans la liste" placeholder="Nom, téléphone, e-mail ou voiture" />} />
        <DataTable columns={columns} data={list.data ? rows : null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id}
          empty={<EmptyState title={text ? "Aucun résultat" : "Aucun chauffeur dans le programme"} description={text ? "Aucun chauffeur ne correspond à cette recherche." : "Ajoutez un chauffeur pour lui offrir la recharge gratuite."} />} />
        {adding && <AgreementCreateDialog windowText={windowText} onClose={closeCreate} onDone={added} />}
        {editing && <AgreementEditDialog agreement={editing} onClose={() => setEditing(null)} onSubmit={saveEdit} />}
        {revoking && (
          <ConfirmDialog isOpen onClose={() => setRevoking(null)} onConfirm={confirmRevoke} isLoading={revokePending} destructive
            title="Retirer ce chauffeur ?" description={`${revoking.driverName} ne bénéficiera plus de la recharge offerte. L'historique de l'accord est conservé.${revokeError ? ` ${revokeError}` : ""}`}
            confirmText="Retirer" cancelText="Annuler" />
        )}
      </Panel>
    </div>
  );
}
