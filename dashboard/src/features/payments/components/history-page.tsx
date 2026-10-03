"use client";

import { useState } from "react";
import { Page, PageHead, Panel } from "@/components/layout/page";
import { Tabs } from "@/components/ui/tabs";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { ProgressionTab } from "@/features/payments/components/progression-tab";
import { TransactionsTab } from "@/features/payments/components/transactions-tab";

type HistoryTab = "progression" | "transactions";
const TABS = [{ value: "progression" as const, label: "Progression" }, { value: "transactions" as const, label: "Transactions" }];

function HistoryContent() {
  const [tab, setTab] = useState<HistoryTab>("progression");
  return (
    <Page>
      <PageHead title="Historique" description="La progression de chaque chauffeur dans son contrat, et toutes les transactions de paiement." />
      <Tabs tabs={TABS} value={tab} onChange={setTab} label="Historique" />
      <Panel>{tab === "progression" ? <ProgressionTab /> : <TransactionsTab />}</Panel>
    </Page>
  );
}

export function HistoryPage() {
  return (
    <RequirePermission permission={PERMISSIONS.PAYMENTS_READ}>
      <HistoryContent />
    </RequirePermission>
  );
}
