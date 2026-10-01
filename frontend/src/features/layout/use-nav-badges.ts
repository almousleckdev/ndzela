"use client";

import { useEffect } from "react";
import { useAsync } from "@/hooks/use-async";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { paymentsService } from "@/features/payments/services/payments.service";

const REFRESH_MS = 60_000;

/** The live counters of the sidebar (drivers in arrears), refreshed every minute while the console is open. */
export function useNavBadges(): { arrears: number } {
  const { can } = usePermissions();
  const allowed = can(PERMISSIONS.PAYMENTS_READ);
  const summary = useAsync(() => (allowed ? paymentsService.summary() : Promise.resolve(null)), [allowed]);
  const { reload } = summary;
  useEffect(() => {
    if (!allowed) return;
    const timer = setInterval(reload, REFRESH_MS);
    return () => clearInterval(timer);
  }, [allowed, reload]);
  return { arrears: summary.data?.late ?? 0 };
}
