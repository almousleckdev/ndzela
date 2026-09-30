"use client";

import { MyAccessCard } from "@/features/profile/components/my-access-card";
import { ProfileForm } from "@/features/profile/components/profile-form";
import { ProfileHero } from "@/features/profile/components/profile-hero";
import { useAuthStore } from "@/features/auth/store/auth.store";

export default function ProfilePage() {
  // The dashboard layout only renders once the profile is loaded.
  const user = useAuthStore((state) => state.user);
  if (!user) return null;

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <ProfileHero user={user} />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <ProfileForm user={user} />
        <MyAccessCard user={user} />
      </div>
    </div>
  );
}
