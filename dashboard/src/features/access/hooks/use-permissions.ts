"use client";

import { useAuthStore } from "@/features/auth/store/auth.store";
import type { Permission } from "@/features/access/lib/permissions";

// Access checks for the signed-in user.
export function usePermissions() {
  const granted = useAuthStore((state) => state.user?.permissions);

  return {
    can: (permission: Permission) => granted?.includes(permission) ?? false,
  };
}
