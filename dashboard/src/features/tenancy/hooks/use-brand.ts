import { useAuthStore } from "@/features/auth/store/auth.store";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import { brandOf } from "@/features/tenancy/lib/brand";

// The brand of the user's country, or of the country a super admin narrowed the view to.
export function useBrand() {
  const user = useAuthStore((state) => state.user);
  const selected = useCountrySelectionStore((state) => state.selectedCode);
  return brandOf(user ? (user.country?.code ?? selected) : null);
}
