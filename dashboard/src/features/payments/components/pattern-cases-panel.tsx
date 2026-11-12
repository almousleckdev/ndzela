"use client";

import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { Panel } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { patternCaseColumns } from "@/features/payments/components/pattern-case-columns";
import { paymentsService } from "@/features/payments/services/payments.service";

// Accounts that look like two cars while a driver is suspended for arrears. Only a hint: no payment links them to the suspended driver.
export function PatternCasesPanel() {
  const list = useAsync(() => paymentsService.suspensionPatterns(), []);

  return (
    <Panel>
      <h2 className="px-4 pt-4 text-base font-semibold text-ink">Comptes qui semblent charger deux véhicules</h2>
      <p className="px-4 pb-2 text-sm text-muted">Sur les 7 derniers jours, pendant qu&apos;un chauffeur est suspendu pour impayés. Rien ne prouve un lien : vérifiez avant toute sanction.</p>
      <DataTable columns={patternCaseColumns} data={list.data ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.account.id}
        empty={<EmptyState title="Aucun compte suspect" description="Aucun compte ne semble charger deux véhicules." />} />
    </Panel>
  );
}
