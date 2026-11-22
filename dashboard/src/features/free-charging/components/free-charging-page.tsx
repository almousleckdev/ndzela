"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAsync } from "@/hooks/use-async";
import { Tabs } from "@/components/ui/tabs";
import { Page, PageHead } from "@/components/layout/page";
import { RequirePermission } from "@/features/access/components/require-permission";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { AgreementsTab } from "@/features/free-charging/components/agreements-tab";
import { UsageTab } from "@/features/free-charging/components/usage-tab";
import { WindowCard } from "@/features/free-charging/components/window-card";
import { describeWindow } from "@/features/free-charging/lib/free-charging-window";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";

const TABS = [{ value: "drivers" as const, label: "Chauffeurs" }, { value: "usage" as const, label: "Historique" }];

export function FreeChargingPage() {
  const [tab, setTab] = useState<"drivers" | "usage">("drivers");
  const { can } = usePermissions();
  const [adding, setAdding] = useState(false);
  const window = useAsync(freeChargingService.window, []);
  const windowText = window.data ? describeWindow(window.data) : undefined;

  return (
    <RequirePermission permission={PERMISSIONS.FREE_CHARGING_READ}>
      <Page>
        <PageHead title="Gratuité" description="Les chauffeurs NDZela qui profitent d'une recharge offerte pendant le créneau gratuit."
          actions={can(PERMISSIONS.FREE_CHARGING_MANAGE) && <Button onClick={() => setAdding(true)}><Plus className="mr-2 h-4 w-4" /> Ajouter un chauffeur</Button>} />
        <WindowCard window={window.data} onChanged={window.reload} />
        <Tabs tabs={TABS} value={tab} onChange={setTab} label="Gratuité" />
        {tab === "drivers" ? <AgreementsTab windowText={windowText} adding={adding} onAddingClose={() => setAdding(false)} /> : <UsageTab />}
      </Page>
    </RequirePermission>
  );
}
