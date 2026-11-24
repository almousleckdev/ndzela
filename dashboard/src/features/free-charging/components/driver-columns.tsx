import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { None, Stack } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatDateTime } from "@/lib/time";
import { eligibilityLabel, STATUS_LABELS, STATUS_VARIANTS } from "@/features/free-charging/lib/free-charging-labels";
import type { Agreement, AgreementStatus, DriverOption } from "@/features/free-charging/types";

const col = columnsFor<DriverOption>();

export type DriverAction = "add" | "edit" | "suspend" | "resume" | "revoke";

interface Options {
  canManage: boolean;
  agreements: Map<string, Agreement>;
  busy: Set<string>;
  onAction: (action: DriverAction, driver: DriverOption) => void;
}

// The active drivers of the Gratuité page: one click adds a driver to the program or removes them.
export function driverColumns({ canManage, agreements, busy, onAction }: Options) {
  const agreementOf = (driver: DriverOption) => (driver.agreementId ? agreements.get(driver.agreementId) : undefined);
  const actions = col.display({
    id: "actions",
    header: () => <span className="sr-only">Actions</span>,
    enableHiding: false,
    cell: (c) => {
      const driver = c.row.original;
      const disabled = busy.has(driver.driverId);
      if (!driver.inProgram) {
        return (
          <div className="flex justify-end">
            <Button size="sm" aria-label={`Ajouter ${driver.fullName}`} disabled={disabled || !driver.linked} title={driver.linked ? undefined : "Compte Zella non lié"} onClick={() => onAction("add", driver)}>Ajouter</Button>
          </div>
        );
      }
      const status = driver.agreementStatus as AgreementStatus | null;
      return (
        <div className="flex justify-end gap-1">
          {agreementOf(driver) && <Button variant="ghost" size="sm" aria-label={`Modifier ${driver.fullName}`} disabled={disabled} onClick={() => onAction("edit", driver)}>Modifier</Button>}
          {status === "ACTIVE" && <Button variant="ghost" size="sm" aria-label={`Suspendre ${driver.fullName}`} disabled={disabled} onClick={() => onAction("suspend", driver)}>Suspendre</Button>}
          {status === "SUSPENDED" && <Button variant="ghost" size="sm" aria-label={`Reprendre ${driver.fullName}`} disabled={disabled} onClick={() => onAction("resume", driver)}>Reprendre</Button>}
          <Button variant="outline" size="sm" className="text-red-600 hover:bg-red-50" aria-label={`Retirer ${driver.fullName}`} disabled={disabled} onClick={() => onAction("revoke", driver)}>Retirer</Button>
        </div>
      );
    },
  });

  return col.columns([
    col.accessor("fullName", { id: "driver", header: "Chauffeur", enableSorting: false, cell: (c) => <Stack primary={c.getValue()} secondary={[c.row.original.phone, c.row.original.email].filter(Boolean).join(" · ")} /> }),
    col.display({ id: "car", header: "Voiture", cell: (c) => ([c.row.original.fleetNumber, c.row.original.plate].filter(Boolean).join(" · ") || <None />) }),
    col.display({
      id: "program",
      header: "Programme",
      cell: (c) => {
        const driver = c.row.original;
        if (driver.inProgram) {
          const status = driver.agreementStatus as AgreementStatus | null;
          return <Badge variant={status ? STATUS_VARIANTS[status] : "success"}>{status ? STATUS_LABELS[status] : "Dans le programme"}</Badge>;
        }
        return driver.linked ? <Badge>Hors programme</Badge> : <Badge variant="warning">Compte Zella non lié</Badge>;
      },
    }),
    col.display({ id: "eligibility", header: "Éligibilité", cell: (c) => { const a = agreementOf(c.row.original); return a ? eligibilityLabel(a.eligibility) : <None />; } }),
    col.display({ id: "unpaidDays", header: "Jours impayés", cell: (c) => { const a = agreementOf(c.row.original); return a ? <span className="tabular-nums">{a.unpaidDays}</span> : <None />; } }),
    col.display({ id: "lastFreeUse", header: "Dernière recharge offerte", cell: (c) => { const a = agreementOf(c.row.original); return a?.lastFreeUse ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(a.lastFreeUse)}</span> : <None />; } }),
    ...(canManage ? [actions] : []),
  ]);
}
