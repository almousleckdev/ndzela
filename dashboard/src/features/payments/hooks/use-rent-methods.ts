import { useAsync } from "@/hooks/use-async";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import { paymentsService } from "@/features/payments/services/payments.service";

// The mobile-money methods rent is paid with in the user's country (Gabon or Mali)
export function useRentMethods() {
  const selected = useCountrySelectionStore((state) => state.selectedCode);
  const operators = useAsync(() => paymentsService.operators(), [selected]);
  return (operators.data ?? []).map((operator) => ({ value: operator.code, label: operator.label }));
}
