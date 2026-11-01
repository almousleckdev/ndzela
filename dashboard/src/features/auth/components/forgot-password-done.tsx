"use client";

import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StepHeader } from "@/features/auth/components/step-header";

export function ForgotPasswordDone({ onBack }: { onBack: () => void }) {
  return (
    <div className="space-y-6 text-center">
      <CheckCircle2 className="mx-auto h-12 w-12 text-primary" />
      <StepHeader
        title="Mot de passe modifié"
        description="Votre mot de passe a été réinitialisé. Vous pouvez maintenant vous connecter."
      />
      <Button size="xl" onClick={onBack} className="w-full">
        Aller à la connexion
      </Button>
    </div>
  );
}
