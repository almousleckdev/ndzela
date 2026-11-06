"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { KpiBand, Page, PageHead, Panel, type Kpi } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { formatFcfa } from "@/lib/money";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { TransactionsTab } from "@/features/payments/components/transactions-tab";
import { LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";

// One driver's payments: where he stands, then every payment he made, with the failed attempts one click away
function Content({ driverId }: { driverId: string }) {
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  const a = account.data;
  const kpis: Kpi[] = [
    { label: "Niveau", value: a?.contractId ? LEVEL_LABELS[a.level] : "-" },
    { label: "Montant dû", value: a ? formatFcfa(a.overdueAmount) : "-", tone: a && a.overdueAmount > 0 ? "bad" : undefined },
    { label: "Crédit", value: a ? formatFcfa(a.credit) : "-" },
    { label: "À temps", value: a?.paidOnTime ?? "-" },
    { label: "En retard", value: a?.paidLate ?? "-" },
    { label: "Impayés", value: a?.unpaid ?? "-", tone: a && a.unpaid > 0 ? "bad" : undefined },
  ];
  return (
    <Page>
      <PageHead
        title={a?.driverName ?? "Historique du chauffeur"}
        description="Les paiements de ce chauffeur et leurs jours réglés."
        actions={(
          <>
            <Link href="/finance/history"><Button variant="outline"><ArrowLeft className="mr-2 h-4 w-4" /> Historique</Button></Link>
            <Link href={`/drivers/${driverId}`}><Button variant="outline">Profil du chauffeur</Button></Link>
          </>
        )}
      />
      <KpiBand items={kpis} />
      <Panel><TransactionsTab driverId={driverId} /></Panel>
    </Page>
  );
}

export function DriverHistoryPage({ driverId }: { driverId: string }) {
  return (
    <RequirePermission permission={PERMISSIONS.PAYMENTS_READ}>
      <Content driverId={driverId} />
    </RequirePermission>
  );
}
