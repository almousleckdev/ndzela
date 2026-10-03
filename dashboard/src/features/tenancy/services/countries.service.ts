import { apiClient } from "@/lib/api/api-client";
import type { Country } from "@/features/auth/types";

export const countriesService = {
  // Skips the country header so the list stays complete.
  list: () => apiClient<Country[]>("/countries", { sendContext: false }),
};
