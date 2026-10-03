import { create } from "zustand";
import { registerHeaderProvider } from "@/lib/api/api-client";
import { useAuthStore } from "@/features/auth/store/auth.store";

const STORAGE_KEY = "ndzela.country";

function readStored(): string | null {
  try {
    return typeof window === "undefined" ? null : window.sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function writeStored(code: string | null): void {
  try {
    if (code) window.sessionStorage.setItem(STORAGE_KEY, code);
    else window.sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage can be blocked (private mode): the selection then lasts until reload.
  }
}

interface CountrySelectionState {
  // The country code the super admin narrowed the view to, or null for "all countries".
  selectedCode: string | null;
  select: (code: string | null) => void;
}

export const useCountrySelectionStore = create<CountrySelectionState>()((set) => ({
  selectedCode: readStored(),
  select: (code) => {
    writeStored(code);
    set({ selectedCode: code });
  },
}));

// Sent on every authenticated request, and only for a global user: the server ignores it for everyone else.
registerHeaderProvider((): Record<string, string> => {
  const user = useAuthStore.getState().user;
  const { selectedCode } = useCountrySelectionStore.getState();
  return user && user.country === null && selectedCode ? { "X-Country": selectedCode } : {};
});
