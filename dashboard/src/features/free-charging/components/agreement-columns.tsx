import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { None } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatDateTime } from "@/lib/time";
import { eligibilityLabel, formatDay, STATUS_LABELS, STATUS_VARIANTS } from "@/features/free-charging/lib/free-charging-labels";
import type { Agreement } from "@/features/free-charging/types";

const col = columnsFor<Agreement>();

export type AgreementAction = "edit" | "suspend" | "resume" | "revoke";

export function agreementColumns(canManage: boolean, onAction: (action: AgreementAction, agreement: Agreement) => void) {
  const actions = [
    col.display({
      id: "actions",
      header: () => <span className="sr-only">Actions</span>,
      enableHiding: false,
      cell: (c) => {
        const agreement = c.row.original;
        if (agreement.status === "REVOKED") return null;
        return (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="sm" aria-label={`Modifier ${agreement.driverName}`} onClick={() => onAction("edit", agreement)}>Modifier</Button>
            {agreement.status === "ACTIVE" && <Button variant="ghost" size="sm" aria-label={`Suspendre ${agreement.driverName}`} onClick={() => onAction("suspend", agreement)}>Suspendre</Button>}
            {agreement.status === "SUSPENDED" && <Button variant="ghost" size="sm" aria-label={`Reprendre ${agreement.driverName}`} onClick={() => onAction("resume", agreement)}>Reprendre</Button>}
            <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" aria-label={`Retirer ${agreement.driverName}`} onClick={() => onAction("revoke", agreement)}>Retirer</Button>
          </div>
        );
      },
    }),
  ];

  return col.columns([
    col.accessor("driverName", { id: "driver", header: "Chauffeur", cell: (c) => <span className="font-medium text-ink">{c.getValue()}</span> }),
    col.display({ id: "car", header: "Voiture", cell: (c) => (c.row.original.fleetNumber ? `${c.row.original.fleetNumber}${c.row.original.plate ? ` · ${c.row.original.plate}` : ""}` : <None />) }),
    col.display({ id: "phone", header: "Téléphone", cell: (c) => c.row.original.phone ?? <None /> }),
    col.accessor("startsOn", { id: "startsOn", header: "Début", cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDay(c.getValue())}</span> }),
    col.accessor("endsOn", { id: "endsOn", header: "Fin", cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDay(c.getValue())}</span> : <None />) }),
    col.accessor("status", { id: "status", header: "Statut", cell: (c) => <Badge variant={STATUS_VARIANTS[c.getValue()]}>{STATUS_LABELS[c.getValue()]}</Badge> }),
    col.accessor("eligibility", { id: "eligibility", header: "Éligibilité", enableSorting: false, cell: (c) => eligibilityLabel(c.getValue()) }),
    col.accessor("unpaidDays", { id: "unpaidDays", header: "Jours impayés", cell: (c) => <span className="tabular-nums">{c.getValue()}</span> }),
    col.accessor("lastFreeUse", { id: "lastFreeUse", header: "Dernière recharge offerte", cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    col.accessor("conditions", { id: "conditions", header: "Conditions", enableSorting: false, cell: (c) => (c.getValue() ? <span className="block max-w-[16rem] truncate" title={c.getValue() as string}>{c.getValue()}</span> : <None />) }),
    ...(canManage ? actions : []),
  ]);
}
