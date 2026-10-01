"use client";

import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { getErrorMessage } from "@/lib/errors";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { DriverWizard } from "@/features/drivers/components/driver-wizard";
import { driversService } from "@/features/drivers/services/drivers.service";

function DriverEditContent({ id }: { id: string }) {
  const driver = useAsync(() => driversService.get(id), [id]);
  if (driver.error) return <EmptyState title="Chauffeur introuvable" description={getErrorMessage(driver.error, "Il a peut-être été supprimé.")} />;
  if (!driver.data) return <Skeleton className="mx-auto h-96 max-w-3xl" />;
  return <DriverWizard driver={driver.data} />;
}

export function DriverEditPage({ id }: { id: string }) {
  return (
    <RequirePermission permission={PERMISSIONS.DRIVERS_UPDATE}>
      <DriverEditContent id={id} />
    </RequirePermission>
  );
}
