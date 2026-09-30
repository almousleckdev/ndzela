import { create } from "zustand";
import type { UserProfile } from "@/features/auth/types";

export type SessionStatus = "unknown" | "authenticated" | "anonymous";

interface AuthState {
  status: SessionStatus;
  /** Kept in memory only: nothing sensitive is ever written to localStorage. */
  accessToken: string | null;
  user: UserProfile | null;
  setAccessToken: (accessToken: string) => void;
  setUser: (user: UserProfile) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>()((set) => ({
  status: "unknown",
  accessToken: null,
  user: null,
  setAccessToken: (accessToken) => set({ accessToken, status: "authenticated" }),
  setUser: (user) => set({ user }),
  clear: () => set({ accessToken: null, user: null, status: "anonymous" }),
}));
