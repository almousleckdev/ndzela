import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { DriverWizard } from "@/features/drivers/components/driver-wizard";

export default function Page() {
  return (
    <RequirePermission permission={PERMISSIONS.DRIVERS_CREATE}>
      <DriverWizard />
    </RequirePermission>
  );
}
