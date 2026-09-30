"use client";

import { ChangePasswordCard } from "@/features/settings/components/change-password-card";
import { MfaStatusCard } from "@/features/settings/components/mfa-status-card";
import { PageHeader } from "@/components/ui/page-header";
import { useAuthStore } from "@/features/auth/store/auth.store";

export default function SettingsPage() {
  const mfaEnabled = useAuthStore((state) => state.user?.mfaEnabled ?? false);

  return (
    <div className="mx-auto max-w-4xl space-y-8">
      <PageHeader title="Paramètres de sécurité" description="Gérez votre mot de passe et consultez l'état de l'authentification à deux facteurs." />
      <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
        <ChangePasswordCard />
        <MfaStatusCard mfaEnabled={mfaEnabled} />
      </div>
    </div>
  );
}
