"use client";

import { useState } from "react";
import { useAsync } from "@/hooks/use-async";
import { Tabs } from "@/components/ui/tabs";
import { Page, PageHead } from "@/components/layout/page";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { DriversTab } from "@/features/free-charging/components/drivers-tab";
import { UsageTab } from "@/features/free-charging/components/usage-tab";
import { WindowCard } from "@/features/free-charging/components/window-card";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";

const TABS = [{ value: "drivers" as const, label: "Chauffeurs" }, { value: "usage" as const, label: "Historique" }];

export function FreeChargingPage() {
  const [tab, setTab] = useState<"drivers" | "usage">("drivers");
  const window = useAsync(freeChargingService.window, []);

  return (
    <RequirePermission permission={PERMISSIONS.FREE_CHARGING_READ}>
      <Page>
        <PageHead title="Gratuité" description="Les chauffeurs actifs. Ajoutez un chauffeur au programme pour lui offrir la recharge gratuite pendant le créneau gratuit, ou retirez-le." />
        <WindowCard window={window.data} onChanged={window.reload} />
        <Tabs tabs={TABS} value={tab} onChange={setTab} label="Gratuité" />
        {tab === "drivers" ? <DriversTab /> : <UsageTab />}
      </Page>
    </RequirePermission>
  );
}
