"use client";

import { toOptions } from "@/lib/options";
import { useCallback, useMemo, useState } from "react";
import { ExportMenu } from "@/components/export-menu";
import { EmptyState } from "@/components/ui/empty-state";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { transactionColumns } from "@/features/payments/components/transaction-columns";
import { useRentMethods } from "@/features/payments/hooks/use-rent-methods";
import { METHOD_LABELS, TRANSACTION_STATUS_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { PaymentMethod, RiderScope, TransactionQuery, TransactionRow, TransactionStatus } from "@/features/payments/types";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";

const SCOPE_LABELS: Record<RiderScope, string> = { CURRENT: "Chauffeurs actuels", FORMER: "Anciens chauffeurs" };
const SCOPE_OPTIONS = toOptions(SCOPE_LABELS, ["CURRENT", "FORMER"]);
const STATUS_OPTIONS = toOptions(TRANSACTION_STATUS_LABELS, ["SUCCESS", "FAILED", "PENDING"]);

// Every payment
export function TransactionsTab() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const methodOptions = useRentMethods();
  const query = useTableQuery();
  const { filters, range } = query;
  const [reversing, setReversing] = useState<TransactionRow | null>(null);
  const params: TransactionQuery = { status: (filters.status || "ALL") as TransactionQuery["status"], method: (filters.method || "ALL") as TransactionQuery["method"], scope: (filters.scope || undefined) as RiderScope | undefined, q: query.q, from: range.from, to: range.to };
  const list = useAsync(() => paymentsService.transactions(params, query.page, query.size), [filters.status, filters.method, filters.scope, query.q, range.from, range.to, query.page, query.size]);
  const receipt = useCallback(async (row: TransactionRow) => {
    try {
      await downloadFile(paymentsService.receiptPath(row.id), `recu-${row.reference}.pdf`);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de télécharger le reçu."), type: "error" });
    }
  }, [addToast]);
  const columns = useMemo(() => transactionColumns(can(PERMISSIONS.PAYMENTS_REVERSE) ? setReversing : undefined, receipt), [can, receipt]);

  const active: ActiveFilter[] = [
    ...(filters.status ? [{ key: "status", label: TRANSACTION_STATUS_LABELS[filters.status as TransactionStatus], onRemove: () => query.setFilter("status", "") }] : []),
    ...(filters.scope ? [{ key: "scope", label: SCOPE_LABELS[filters.scope as RiderScope], onRemove: () => query.setFilter("scope", "") }] : []),
    ...(filters.method ? [{ key: "method", label: METHOD_LABELS[filters.method as PaymentMethod], onRemove: () => query.setFilter("method", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher une transaction" placeholder="Chauffeur, téléphone ou référence" value={query.search} onChange={query.setSearch} />
          <FilterPill label="Statut" value={filters.status ?? ""} options={STATUS_OPTIONS} onChange={(value) => query.setFilter("status", value)} />
          <FilterPill label="Chauffeurs" value={filters.scope ?? ""} options={SCOPE_OPTIONS} onChange={(value) => query.setFilter("scope", value)} />
          <FilterPill label="Méthode" value={filters.method ?? ""} options={methodOptions} onChange={(value) => query.setFilter("method", value)} />
          <DateRangePicker value={range} onChange={query.setRange} />
        </>
      )} end={can(PERMISSIONS.PAYMENTS_EXPORT) && (
        <ExportMenu path={(format) => paymentsService.exportPath({ type: "PAYMENT", method: params.method, q: query.q, from: range.from, to: range.to }, format)} filename="paiements" failure="Impossible d'exporter les paiements." />
      )} />
      <ActiveFilters filters={active} onClear={query.reset} />
      <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id} numeric={["amount"]}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: [], onSortingChange: () => {} }}
        empty={<EmptyState title="Aucune transaction" description="Aucun paiement ne correspond à ces filtres. Élargissez la période ou effacez un filtre." />} />
      {reversing && (
        <ReasonDialog title="Annuler ce paiement ?" description={`Paiement de ${formatFcfa(reversing.amount)} saisi à la main pour ${reversing.driverName}. Utilisez-le seulement s'il a été saisi par erreur : une écriture d'annulation est ajoutée et le paiement d'origine reste visible.`}
          confirmText="Annuler le paiement" destructive onClose={() => setReversing(null)}
          onConfirm={async (reason) => {
            await paymentsService.reverse(reversing.id, reason);
            setReversing(null);
            addToast({ title: "Succès", description: "Paiement annulé.", type: "success" });
            list.reload();
          }} />
      )}
    </>
  );
}
