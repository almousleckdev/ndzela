"use client";

import { useEffect } from "react";
import { useAsync } from "@/hooks/use-async";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { inquiriesService } from "@/features/inquiries/services/inquiries.service";
import { paymentsService } from "@/features/payments/services/payments.service";

const REFRESH_MS = 60_000;

// The live counters of the sidebar (drivers in arrears, new applications and messages)
export function useNavBadges(): { arrears: number; inquiries: number } {
  const { can } = usePermissions();
  const allowed = can(PERMISSIONS.PAYMENTS_READ);
  const summary = useAsync(() => (allowed ? paymentsService.summary() : Promise.resolve(null)), [allowed]);
  const mayReadInquiries = can(PERMISSIONS.INQUIRIES_READ);
  const inquiries = useAsync(() => (mayReadInquiries ? inquiriesService.newCount() : Promise.resolve(0)), [mayReadInquiries]);
  const { reload } = summary;
  const reloadInquiries = inquiries.reload;
  useEffect(() => {
    if (!allowed && !mayReadInquiries) return;
    const timer = setInterval(() => { if (allowed) reload(); if (mayReadInquiries) reloadInquiries(); }, REFRESH_MS);
    return () => clearInterval(timer);
  }, [allowed, mayReadInquiries, reload, reloadInquiries]);
  return { arrears: summary.data?.late ?? 0, inquiries: inquiries.data ?? 0 };
}
