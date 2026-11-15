"use client";

import { useState } from "react";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { SearchField } from "@/components/data-table/search-field";
import { ExportMenu } from "@/components/export-menu";
import { KpiBand, Page, PageHead, Panel, PanelHead, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { NO_RANGE } from "@/lib/date-range";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { earningsKpis } from "@/features/payments/components/earnings-kpis";
import { EarningsDriversTable } from "@/features/payments/components/earnings-drivers-table";
import { EarningsMonthsTable } from "@/features/payments/components/earnings-months-table";
import { earningsService } from "@/features/payments/services/earnings.service";

function EarningsContent() {
  const [range, setRange] = useState(NO_RANGE);
  const [search, setSearch] = useState("");
  const summary = useAsync(() => earningsService.summary(range), [range.from, range.to]);
  const drivers = useAsync(() => earningsService.drivers(range, search), [range.from, range.to, search]);

  return (
    <Page>
      <PageHead title="Total encaissé" description="Tout l'argent encaissé par NDZela depuis le début, anciens chauffeurs compris."
        actions={<ExportMenu range={range} path={(format, choice) => earningsService.exportPath(choice.range, format)} filename="total-encaisse" failure="Impossible d'exporter le total encaissé." />} />
      <KpiBand items={earningsKpis(summary.data)} />
      <Panel>
        <Toolbar start={<DateRangePicker value={range} onChange={setRange} label="Période" />} />
      </Panel>
      <Panel>
        <PanelHead title="Mois par mois" hint="Dans le fuseau horaire du pays" />
        <EarningsMonthsTable months={summary.data?.months ?? null} />
      </Panel>
      <Panel>
        <PanelHead title="Par chauffeur" hint="Chauffeurs actuels et anciens" />
        <Toolbar start={<SearchField label="Rechercher un chauffeur" placeholder="Nom du chauffeur" value={search} onChange={setSearch} />} />
        <EarningsDriversTable rows={drivers.data?.items ?? null} error={drivers.error} onRetry={drivers.reload} />
      </Panel>
    </Page>
  );
}

export function EarningsPage() {
  return (
    <RequirePermission permission={PERMISSIONS.EARNINGS_READ}>
      <EarningsContent />
    </RequirePermission>
  );
}
