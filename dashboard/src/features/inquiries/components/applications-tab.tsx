"use client";

import { toOptions } from "@/lib/options";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";
import { None, Stack, Status } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { formatDateTime } from "@/lib/time";
import { ApplicationDialog } from "@/features/inquiries/components/application-dialog";
import { APPLICATION_STATUS_LABELS, APPLICATION_STATUS_TONE } from "@/features/inquiries/lib/labels";
import { inquiriesService } from "@/features/inquiries/services/inquiries.service";
import type { Application, ApplicationStatus } from "@/features/inquiries/types";

const col = columnsFor<Application>();
const STATUS_OPTIONS = toOptions(APPLICATION_STATUS_LABELS);

// The driver applications received from the website, newest first.
export function ApplicationsTab({ onChanged }: { onChanged: () => void }) {
  const query = useTableQuery();
  const status = (query.filters.status ?? "") as ApplicationStatus | "";
  const list = useAsync(() => inquiriesService.applications(status, query.q, query.page, query.size), [status, query.q, query.page, query.size]);
  const [opened, setOpened] = useState<Application | null>(null);

  const columns = useMemo(() => col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Reçue le", enableSorting: false, cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.display({ id: "candidate", header: "Candidat", cell: (c) => <Stack primary={`${c.row.original.firstName} ${c.row.original.lastName}`} secondary={c.row.original.phone} /> }),
    col.accessor("city", { id: "city", header: "Ville", enableSorting: false, cell: (c) => c.getValue() ?? <None /> }),
    col.accessor("licenceNumber", { id: "licence", header: "Permis", enableSorting: false, cell: (c) => c.getValue() ?? <None /> }),
    col.accessor("status", { id: "status", header: "Suivi", enableSorting: false, cell: (c) => <Status tone={APPLICATION_STATUS_TONE[c.getValue()]}>{APPLICATION_STATUS_LABELS[c.getValue()]}</Status> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => <div className="flex justify-end"><Button variant="ghost" size="sm" onClick={() => setOpened(c.row.original)}>Ouvrir</Button></div> }),
  ]), []);

  return (
    <>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher une candidature" placeholder="Nom, e-mail ou téléphone" value={query.search} onChange={query.setSearch} />
          <FilterPill label="Suivi" value={status} options={STATUS_OPTIONS} onChange={(value) => query.setFilter("status", value)} />
        </>
      )} />
      <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: [], onSortingChange: () => {} }}
        empty={<EmptyState title="Aucune candidature" description="Les candidatures envoyées depuis le site apparaissent ici." />} />
      {opened && <ApplicationDialog application={opened} onClose={() => setOpened(null)} onSaved={() => { setOpened(null); list.reload(); onChanged(); }} />}
    </>
  );
}
