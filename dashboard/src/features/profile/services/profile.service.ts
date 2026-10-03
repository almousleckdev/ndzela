import { apiClient } from "@/lib/api/api-client";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { UserProfile } from "@/features/auth/types";
import type { ProfileValues } from "@/features/profile/schemas/profile";

// Loads the current user and keeps the auth store in sync.
export async function getProfile(): Promise<UserProfile> {
  const user = await apiClient<UserProfile>("/profile/me");
  useAuthStore.getState().setUser(user);
  return user;
}

export async function updateProfile(values: ProfileValues): Promise<UserProfile> {
  await apiClient("/profile/me", {
    method: "PUT",
    body: {
      firstName: values.firstName || null,
      lastName: values.lastName || null,
      phoneNumber: values.phoneNumber || null,
    },
  });
  return getProfile();
}

// The server revokes every session on success, so the caller must sign the user out.
export function changePassword(currentPassword: string, newPassword: string) {
  return apiClient("/profile/password", { method: "PUT", body: { currentPassword, newPassword } });
}

// Replaces the authenticator.
export function resetMfa(password: string, code: string) {
  return apiClient("/profile/mfa/reset", { method: "POST", body: { password, code } });
}

export async function uploadPicture(file: File): Promise<UserProfile> {
  const form = new FormData();
  form.append("file", file);
  await apiClient("/profile/picture", { method: "POST", body: form });
  return getProfile();
}

export async function removePicture(): Promise<UserProfile> {
  await apiClient("/profile/picture", { method: "DELETE" });
  return getProfile();
}
