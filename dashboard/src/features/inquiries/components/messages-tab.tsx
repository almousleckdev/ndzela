"use client";

import { toOptions } from "@/lib/options";
import { useMemo, useState } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { Button } from "@/components/ui/button";
import { Stack, Status } from "@/components/data-table/cells";
import { BulkDelete } from "@/components/data-table/bulk-delete";
import { DataTable } from "@/components/data-table/data-table";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { formatDateTime } from "@/lib/time";
import { MessageDialog } from "@/features/inquiries/components/message-dialog";
import { MESSAGE_STATUS_LABELS, MESSAGE_STATUS_TONE } from "@/features/inquiries/lib/labels";
import { inquiriesService } from "@/features/inquiries/services/inquiries.service";
import type { ContactMessage, MessageStatus } from "@/features/inquiries/types";

const col = columnsFor<ContactMessage>();
const STATUS_OPTIONS = toOptions(MESSAGE_STATUS_LABELS);

// The messages sent from the website's contact page, newest first.
export function MessagesTab({ onChanged }: { onChanged: () => void }) {
  const query = useTableQuery();
  const status = (query.filters.status ?? "") as MessageStatus | "";
  const list = useAsync(() => inquiriesService.messages(status, query.q, query.page, query.size), [status, query.q, query.page, query.size]);

  const [opened, setOpened] = useState<ContactMessage | null>(null);
  const { can } = usePermissions();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const columns = useMemo(() => col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Reçu le", enableSorting: false, cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.display({ id: "from", header: "De", cell: (c) => <Stack primary={c.row.original.name} secondary={[c.row.original.phone, c.row.original.email].filter(Boolean).join(" · ") || undefined} /> }),
    col.display({ id: "message", header: "Message", cell: (c) => <div className="max-w-md"><p className="font-medium">{c.row.original.subject}</p><p className="line-clamp-2 text-muted">{c.row.original.message}</p></div> }),
    col.accessor("status", { id: "status", header: "Statut", enableSorting: false, cell: (c) => <Status tone={MESSAGE_STATUS_TONE[c.getValue()]}>{MESSAGE_STATUS_LABELS[c.getValue()]}</Status> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => <div className="flex justify-end"><Button variant="ghost" size="sm" onClick={() => setOpened(c.row.original)}>Ouvrir</Button></div> }),
  ]), []);

  return (
    <>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher un message" placeholder="Nom, objet, e-mail ou téléphone" value={query.search} onChange={query.setSearch} />
          <FilterPill label="Statut" value={status} options={STATUS_OPTIONS} onChange={(value) => query.setFilter("status", value)} />
        </>
      )} />
      {can(PERMISSIONS.INQUIRIES_MANAGE) && (
        <BulkDelete items={list.data?.items ?? []} selected={selected} onSelectedChange={setSelected} idOf={(a) => a.id} nameOf={(a) => `${a.name} · ${a.subject}`}
          remove={(a) => inquiriesService.deleteMessage(a.id)} noun={{ one: "message", many: "messages" }} note="La suppression est définitive." onDone={() => { list.reload(); onChanged(); }} />
      )}
      <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id} selection={can(PERMISSIONS.INQUIRIES_MANAGE) ? { selected, onChange: setSelected } : undefined}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: [], onSortingChange: () => {} }}
        empty={<EmptyState title="Aucun message" description="Les messages envoyés depuis la page contact du site apparaissent ici." />} />
      {opened && (
        <MessageDialog message={opened} onClose={() => setOpened(null)}
          onSaved={() => { setOpened(null); list.reload(); onChanged(); }}
          onDeleted={() => { setOpened(null); list.reload(); onChanged(); }} />
      )}
    </>
  );
}
