"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { refreshSession } from "@/lib/api/api-client";
import { getProfile } from "@/features/profile/services/profile.service";
import { useAuthStore } from "@/features/auth/store/auth.store";

// Guards a page
export function useRequireSession(): boolean {
  const router = useRouter();
  const status = useAuthStore((state) => state.status);
  const hasUser = useAuthStore((state) => state.user !== null);

  useEffect(() => {
    let cancelled = false;
    async function restore() {
      if (status === "unknown" && !(await refreshSession())) {
        useAuthStore.getState().clear();
      }
      if (cancelled) return;
      const current = useAuthStore.getState();
      if (current.status === "authenticated" && !current.user) {
        try {
          await getProfile();
        } catch {
          current.clear();
        }
      }
    }
    restore();
    return () => {
      cancelled = true;
    };
  }, [status]);

  useEffect(() => {
    if (status === "anonymous") router.replace("/login");
  }, [status, router]);

  return status === "authenticated" && hasUser;
}
