"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, ChevronsUpDown, Columns3, RotateCw } from "lucide-react";
import { useTable, type PaginationState, type RowData, type SortingState, type ColumnVisibilityState } from "@tanstack/react-table";
import { Popover } from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { getErrorMessage } from "@/lib/errors";
import { cn } from "@/lib/utils";
import { dataTableFeatures, type Col } from "@/components/data-table/features";

const EMPTY: never[] = [];
export const PAGE_SIZES = [10, 20, 50, 100];

export interface ServerState {
  total: number;
  page: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  sorting: SortingState;
  onSortingChange: (sorting: SortingState) => void;
}

interface DataTableProps<T extends RowData> {
  columns: Col<T>[];
  data: T[] | null;
  // Present for tables whose rows, order and page come from the server
  server?: ServerState;
  error?: unknown;
  onRetry?: () => void;
  getRowId?: (row: T) => string;
  // A Tailwind background class for the 3 px marker on the row's left edge (arrears level
  rowMarker?: (row: T) => string | undefined;
  // Ids of columns whose header and cells are right-aligned (amounts).
  numeric?: string[];
  empty: React.ReactNode;
  initialSorting?: SortingState;
  // Ids of columns hidden at first; the user can show them from "Colonnes".
  initiallyHidden?: string[];
  className?: string;
  // Adds a checkbox column (and "select the page"); the caller owns the selected ids, which need `getRowId`.
  selection?: { selected: ReadonlySet<string>; onChange: (next: Set<string>) => void };
}

// The one table of the console
export function DataTable<T extends RowData>({ columns, data, server, error, onRetry, getRowId, rowMarker, numeric = [], empty, initialSorting = [], initiallyHidden = [], className, selection }: DataTableProps<T>) {
  const [localSorting, setLocalSorting] = useState<SortingState>(initialSorting);
  const [localPagination, setLocalPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [visibility, setVisibility] = useState<ColumnVisibilityState>(() => Object.fromEntries(initiallyHidden.map((id) => [id, false])));

  const sorting = server ? server.sorting : localSorting;
  const pagination: PaginationState = server ? { pageIndex: server.page, pageSize: server.size } : localPagination;

  const table = useTable({
    features: dataTableFeatures,
    columns,
    data: data ?? EMPTY,
    getRowId: getRowId ? (row) => getRowId(row as T) : undefined,
    manualSorting: Boolean(server),
    manualPagination: Boolean(server),
    rowCount: server ? server.total : undefined,
    state: { sorting, pagination, columnVisibility: visibility },
    onSortingChange: (updater) => {
      const next = typeof updater === "function" ? updater(sorting) : updater;
      if (server) server.onSortingChange(next); else setLocalSorting(next);
    },
    onPaginationChange: (updater) => {
      const next = typeof updater === "function" ? updater(pagination) : updater;
      if (server) {
        if (next.pageSize !== pagination.pageSize) server.onSizeChange(next.pageSize); else server.onPageChange(next.pageIndex);
      } else setLocalPagination(next);
    },
    onColumnVisibilityChange: (updater) => setVisibility((current: ColumnVisibilityState) => (typeof updater === "function" ? updater(current) : updater)),
  });

  const rows = table.getRowModel().rows;
  const total = server ? server.total : (data?.length ?? 0);
  const pageCount = Math.max(1, Math.ceil(total / pagination.pageSize));
  const first = total === 0 ? 0 : pagination.pageIndex * pagination.pageSize + 1;
  const last = Math.min(total, (pagination.pageIndex + 1) * pagination.pageSize);
  const pageIds = selection ? rows.map((row) => row.id) : [];
  const allOnPage = pageIds.length > 0 && pageIds.every((id) => selection?.selected.has(id));
  const togglePage = () => {
    if (!selection) return;
    const next = new Set(selection.selected);
    for (const id of pageIds) { if (allOnPage) next.delete(id); else next.add(id); }
    selection.onChange(next);
  };
  const toggleOne = (id: string) => {
    if (!selection) return;
    const next = new Set(selection.selected);
    if (next.has(id)) next.delete(id); else next.add(id);
    selection.onChange(next);
  };
  const canHide = table.getAllLeafColumns().filter((column) => column.getCanHide());

  return (
    <div className={className}>
      {error ? (
        <div className="flex flex-col items-center gap-3 px-6 py-14 text-center">
          <p className="text-sm font-medium text-ink">Le tableau n&apos;a pas pu être chargé.</p>
          <p className="max-w-sm text-sm text-muted">{getErrorMessage(error, "Vérifiez votre connexion puis réessayez.")}</p>
          {onRetry && <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-md border border-line px-3 py-1.5 text-sm font-medium hover:bg-slate-50"><RotateCw className="h-4 w-4" /> Réessayer</button>}
        </div>
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] border-collapse text-left text-sm">
              <thead className="sticky top-0 z-10 bg-white">
                {table.getHeaderGroups().map((group) => (
                  <tr key={group.id}>
                    {selection && (
                      <th scope="col" className="w-10 border-b border-line py-2.5 pl-4 pr-1">
                        <input type="checkbox" aria-label="Sélectionner toute la page" checked={allOnPage} onChange={togglePage} disabled={pageIds.length === 0} className="h-4 w-4 accent-[#F97316]" />
                      </th>
                    )}
                    {group.headers.map((header) => {
                      const right = numeric.includes(header.column.id);
                      const sort = header.column.getIsSorted();
                      const sortable = header.column.getCanSort();
                      return (
                        <th key={header.id} scope="col" aria-sort={sort === "asc" ? "ascending" : sort === "desc" ? "descending" : undefined}
                          className={cn("whitespace-nowrap border-b border-line px-3.5 py-2.5 text-xs font-medium text-muted first:pl-4 last:pr-4", right && "text-right")}>
                          {header.isPlaceholder ? null : sortable ? (
                            <button type="button" onClick={header.column.getToggleSortingHandler()} className={cn("-mx-1.5 inline-flex items-center gap-1 rounded px-1.5 py-0.5 hover:bg-slate-100 hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary", sort && "text-ink")}>
                              <table.FlexRender header={header} />
                              {sort === "asc" ? <ArrowUp className="h-3.5 w-3.5" /> : sort === "desc" ? <ArrowDown className="h-3.5 w-3.5" /> : <ChevronsUpDown className="h-3.5 w-3.5 opacity-40" />}
                            </button>
                          ) : <table.FlexRender header={header} />}
                        </th>
                      );
                    })}
                  </tr>
                ))}
              </thead>
              <tbody>
                {data === null ? (
                  Array.from({ length: 6 }, (_, index) => (
                    <tr key={index}><td colSpan={Math.max(table.getVisibleLeafColumns().length + (selection ? 1 : 0), 1)} className="border-b border-line/70 px-4 py-3"><Skeleton className="h-5 w-full" /></td></tr>
                  ))
                ) : rows.map((row) => {
                  const marker = rowMarker?.(row.original as T);
                  return (
                    <tr key={row.id} className={cn("group relative border-b border-line/70 transition-colors last:border-b-0 hover:bg-slate-50/80", selection?.selected.has(row.id) && "bg-orange-50/60")}>
                      {selection && (
                        <td className="relative w-10 py-3 pl-4 pr-1 align-middle">
                          {marker && <span aria-hidden className={cn("absolute inset-y-0 left-0 w-[3px]", marker)} />}
                          <input type="checkbox" aria-label="Sélectionner la ligne" checked={selection.selected.has(row.id)} onChange={() => toggleOne(row.id)} className="h-4 w-4 accent-[#F97316]" />
                        </td>
                      )}
                      {row.getVisibleCells().map((cell, index) => (
                        <td key={cell.id} className={cn("relative whitespace-nowrap px-3.5 py-3 align-middle text-ink first:pl-4 last:pr-4", numeric.includes(cell.column.id) && "text-right tabular-nums")}>
                          {index === 0 && marker && !selection && <span aria-hidden className={cn("absolute inset-y-0 left-0 w-[3px]", marker)} />}
                          <table.FlexRender cell={cell} />
                        </td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {data !== null && rows.length === 0 && <div className="border-t border-line">{empty}</div>}
        </>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line px-4 py-2.5 text-sm text-muted">
        <div className="flex items-center gap-3">
          {canHide.length > 0 && (
            <Popover label="Choisir les colonnes" trigger={<><Columns3 className="h-4 w-4" /> Colonnes</>} triggerClassName="h-8 px-2.5 text-xs" panelClassName="w-56 p-1.5">
              {() => (
                <ul>
                  {canHide.map((column) => (
                    <li key={column.id}>
                      <label className="flex cursor-pointer items-center gap-2.5 rounded px-2 py-1.5 text-sm text-ink hover:bg-slate-50">
                        <input type="checkbox" checked={column.getIsVisible()} onChange={column.getToggleVisibilityHandler()} className="h-4 w-4 accent-[#F97316]" />
                        {typeof column.columnDef.header === "string" ? column.columnDef.header : column.id}
                      </label>
                    </li>
                  ))}
                </ul>
              )}
            </Popover>
          )}
          <span className="tabular-nums">{total === 0 ? "Aucun résultat" : `${first}–${last} sur ${total}`}</span>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs">
            Lignes par page
            <select value={pagination.pageSize} onChange={(event) => table.setPageSize(Number(event.target.value))} className="h-8 rounded-md border border-line bg-white px-2 text-sm text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary">
              {PAGE_SIZES.map((size) => <option key={size} value={size}>{size}</option>)}
            </select>
          </label>
          <div className="flex items-center gap-1">
            <button type="button" aria-label="Page précédente" disabled={pagination.pageIndex === 0} onClick={() => table.previousPage()} className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-line text-ink hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"><ChevronLeft className="h-4 w-4" /></button>
            <span className="min-w-[4.5rem] text-center text-xs tabular-nums">Page {pagination.pageIndex + 1} / {pageCount}</span>
            <button type="button" aria-label="Page suivante" disabled={pagination.pageIndex + 1 >= pageCount} onClick={() => table.nextPage()} className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-line text-ink hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"><ChevronRight className="h-4 w-4" /></button>
          </div>
        </div>
      </div>
    </div>
  );
}
