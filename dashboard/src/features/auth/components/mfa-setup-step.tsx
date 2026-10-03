import Image from "next/image";
import { Button } from "@/components/ui/button";
import type { MfaSetupResponse } from "@/features/auth/types";
import { StepHeader } from "@/features/auth/components/step-header";

interface MfaSetupStepProps {
  setup: MfaSetupResponse;
  onContinue: () => void;
}

export function MfaSetupStep({ setup, onContinue }: MfaSetupStepProps) {
  return (
    <div className="space-y-6">
      <StepHeader
        title="Configurer l'Authentificateur"
        description="Scannez ce code QR avec Google Authenticator ou Authy pour sécuriser votre compte."
      />
      <div className="flex justify-center rounded-xl border border-slate-200 bg-white p-4 shadow-md">
        <Image src={setup.qrCodeDataUri} alt="Code QR d'authentification" width={192} height={192} unoptimized />
      </div>
      <p className="text-center text-xs text-slate-700">
        Impossible de scanner ? Saisissez cette clé :
        <code className="mt-1 block break-all font-mono text-sm font-semibold text-slate-900">{setup.secret}</code>
      </p>
      <Button variant="outline" size="xl" onClick={onContinue} className="w-full">
        J&apos;ai scanné le code
      </Button>
    </div>
  );
}
