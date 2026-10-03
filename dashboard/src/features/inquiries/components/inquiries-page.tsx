"use client";

import { useState } from "react";
import { Page, PageHead, Panel } from "@/components/layout/page";
import { Tabs } from "@/components/ui/tabs";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { ApplicationsTab } from "@/features/inquiries/components/applications-tab";
import { MessagesTab } from "@/features/inquiries/components/messages-tab";

type InquiryTab = "applications" | "messages";
const TABS = [{ value: "applications" as const, label: "Candidatures" }, { value: "messages" as const, label: "Messages" }];

function InquiriesContent() {
  const [tab, setTab] = useState<InquiryTab>("applications");
  const noop = () => {};
  return (
    <Page>
      <PageHead title="Candidatures et messages" description="Ce que les visiteurs du site public ont envoyé : candidatures de chauffeurs avec leurs documents, et messages de la page contact." />
      <Tabs tabs={TABS} value={tab} onChange={setTab} label="Candidatures et messages" />
      <Panel>{tab === "applications" ? <ApplicationsTab onChanged={noop} /> : <MessagesTab onChanged={noop} />}</Panel>
    </Page>
  );
}

export function InquiriesPage() {
  return (
    <RequirePermission permission={PERMISSIONS.INQUIRIES_READ}>
      <InquiriesContent />
    </RequirePermission>
  );
}
