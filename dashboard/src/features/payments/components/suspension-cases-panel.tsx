"use client";

import { useState } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { Panel } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { suspensionCaseColumns } from "@/features/payments/components/suspension-case-columns";
import { paymentsService } from "@/features/payments/services/payments.service";

const PAGE_SIZE = 20;

// Attempts to help a suspended driver charge, newest first. For staff only: the other account is shown only as far as the Zella app allows.
export function SuspensionCasesPanel() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(PAGE_SIZE);
  const list = useAsync(() => paymentsService.suspensionCases(page, size), [page, size]);

  return (
    <Panel>
      <h2 className="px-4 pt-4 text-base font-semibold text-ink">Aides détectées pendant une suspension</h2>
      <p className="px-4 pb-2 text-sm text-muted">Une recharge payée avec le numéro d&apos;un chauffeur suspendu a été refusée. Les indices sont à vérifier avant toute sanction.</p>
      <DataTable columns={suspensionCaseColumns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id}
        server={{ total: list.data?.total ?? 0, page, size, onPageChange: setPage, onSizeChange: setSize, sorting: [], onSortingChange: () => undefined }}
        empty={<EmptyState title="Aucune aide détectée" description="Aucun compte n'a servi à aider un chauffeur suspendu." />} />
    </Panel>
  );
}
