"use client";

import { useState } from "react";
import { Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Panel } from "@/components/layout/page";
import { useToast } from "@/hooks/use-toast";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { WindowDialog } from "@/features/free-charging/components/window-dialog";
import { describeWindow } from "@/features/free-charging/lib/free-charging-window";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { FreeWindow, FreeWindowInput } from "@/features/free-charging/types";

interface WindowCardProps {
  window: FreeWindow | null;
  onChanged: () => void;
}

export function WindowCard({ window, onChanged }: WindowCardProps) {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const [editing, setEditing] = useState(false);

  const save = async (input: FreeWindowInput) => {
    await freeChargingService.changeWindow(input);
    addToast({ title: "Succès", description: "Créneau gratuit modifié.", type: "success" });
    setEditing(false);
    onChanged();
  };

  return (
    <Panel>
      <div className="flex flex-wrap items-center justify-between gap-3 p-4">
        <div className="flex items-center gap-3">
          <Clock className="h-5 w-5 text-muted" aria-hidden="true" />
          <div>
            <p className="text-sm font-semibold text-slate-900">Créneau gratuit</p>
            <p className="text-sm text-muted">{window ? `Recharge gratuite ${describeWindow(window)}.` : "Chargement du créneau…"}</p>
          </div>
        </div>
        {window && can(PERMISSIONS.FREE_CHARGING_MANAGE) && <Button variant="outline" onClick={() => setEditing(true)}>Modifier le créneau</Button>}
      </div>
      {editing && window && <WindowDialog window={window} onClose={() => setEditing(false)} onSubmit={save} />}
    </Panel>
  );
}
