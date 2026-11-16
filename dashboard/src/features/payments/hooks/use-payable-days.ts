import { useAsync } from "@/hooks/use-async";
import { paymentsService } from "@/features/payments/services/payments.service";

// The days staff can pay for a driver; null while loading
export function usePayableDays(driverId: string) {
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  return account.isLoading ? null : (account.data?.payableDays ?? []);
}
