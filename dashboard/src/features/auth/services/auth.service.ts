import { apiClient } from "@/lib/api/api-client";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { AuthResponse, LoginStepResponse, MfaSetupResponse } from "@/features/auth/types";
import { getProfile } from "@/features/profile/services/profile.service";

const PUBLIC = { requiresAuth: false } as const;

export const authService = {
  login: (email: string, password: string) =>
    apiClient<LoginStepResponse>("/auth/login", { method: "POST", body: { email, password }, ...PUBLIC }),

  resendOtp: (challengeToken: string) =>
    apiClient<LoginStepResponse>("/auth/resend-otp", { method: "POST", body: { challengeToken }, ...PUBLIC }),

  verifyOtp: (challengeToken: string, otp: string) =>
    apiClient<LoginStepResponse>("/auth/verify-otp", { method: "POST", body: { challengeToken, otp }, ...PUBLIC }),

  beginMfaSetup: (challengeToken: string) =>
    apiClient<MfaSetupResponse>("/auth/mfa/setup", { method: "POST", body: { challengeToken }, ...PUBLIC }),

  // Final login step
  async verifyTotp(challengeToken: string, code: string): Promise<void> {
    const { accessToken } = await apiClient<AuthResponse>("/auth/verify-totp", {
      method: "POST",
      body: { challengeToken, code },
      ...PUBLIC,
    });
    useAuthStore.getState().setAccessToken(accessToken);
    await getProfile();
  },

  forgotPassword: (email: string) =>
    apiClient("/auth/forgot-password", { method: "POST", body: { email }, ...PUBLIC }),

  resetPasswordWithCode: (email: string, code: string, newPassword: string) =>
    apiClient("/auth/forgot-password/reset", { method: "POST", body: { email, code, newPassword }, ...PUBLIC }),

  // Accepting an invitation link (a brand-new account choosing its first password).
  resetPassword: (token: string, newPassword: string) =>
    apiClient("/auth/reset-password", { method: "POST", body: { token, newPassword }, ...PUBLIC }),

  // Revokes the session server-side (best effort) and forgets it locally.
  async logout(): Promise<void> {
    try {
      await apiClient("/auth/logout", { method: "POST", ...PUBLIC });
    } finally {
      useAuthStore.getState().clear();
    }
  },
};
