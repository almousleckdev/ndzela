"use client";

import { Lock, Mail } from "lucide-react";
import { useLoginFlow } from "@/features/auth/hooks/use-login-flow";
import { CodeStep } from "@/features/auth/components/code-step";
import { CredentialsStep } from "@/features/auth/components/credentials-step";
import { ForgotPasswordSent, ForgotPasswordStep } from "@/features/auth/components/forgot-password-step";
import { MfaSetupStep } from "@/features/auth/components/mfa-setup-step";

export function LoginFlow() {
  const flow = useLoginFlow();
  const { step } = flow;

  switch (step.name) {
    case "CREDENTIALS":
      return (
        <CredentialsStep
          onSubmit={flow.submitCredentials}
          onForgotPassword={() => flow.goTo({ name: "FORGOT_PASSWORD" })}
        />
      );
    case "FORGOT_PASSWORD":
      return <ForgotPasswordStep onSubmit={flow.submitForgotPassword} onBack={flow.backToStart} />;
    case "FORGOT_PASSWORD_SENT":
      return <ForgotPasswordSent onBack={flow.backToStart} />;
    case "OTP":
      return (
        <CodeStep
          key="otp"
          icon={Mail}
          title="Vérifiez votre email"
          description="Entrez le code à 6 chiffres envoyé à votre adresse email."
          submitLabel="Vérifier le code"
          onSubmit={(code) => flow.submitOtp(step.challengeToken, code)}
          resend={{ initialSeconds: step.resendInSeconds, onResend: () => flow.resendOtp(step.challengeToken) }}
        />
      );
    case "MFA_SETUP":
      return (
        <MfaSetupStep
          setup={step.setup}
          onContinue={() => flow.goTo({ name: "TOTP", challengeToken: step.challengeToken })}
        />
      );
    case "TOTP":
      return (
        <CodeStep
          key="totp"
          icon={Lock}
          title="Code d'Authentification"
          description="Entrez le code à 6 chiffres de votre application d'authentification."
          submitLabel="Terminer la connexion"
          onSubmit={(code) => flow.submitTotp(step.challengeToken, code)}
        />
      );
  }
}
