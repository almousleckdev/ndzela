"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage, isSessionExpired } from "@/lib/errors";
import { authService } from "@/features/auth/services/auth.service";
import type { CredentialsValues, ForgotPasswordValues } from "@/features/auth/schemas/auth";
import type { MfaSetupResponse } from "@/features/auth/types";

export type LoginFlowStep =
  | { name: "CREDENTIALS" }
  | { name: "FORGOT_PASSWORD" }
  | { name: "FORGOT_PASSWORD_SENT" }
  | { name: "OTP"; challengeToken: string; resendInSeconds: number }
  | { name: "MFA_SETUP"; challengeToken: string; setup: MfaSetupResponse }
  | { name: "TOTP"; challengeToken: string };

const START: LoginFlowStep = { name: "CREDENTIALS" };

/**
 * Drives login: password -> emailed OTP -> (first time only: authenticator enrolment) -> authenticator code.
 * The server decides which step comes next and every step is bound to a server-side challenge token.
 */
export function useLoginFlow() {
  const [step, setStep] = useState<LoginFlowStep>(START);
  const { addToast } = useToast();
  const router = useRouter();

  /** Runs a step's request; on failure shows a message, and restarts the flow when the server session is gone. */
  async function attempt(action: () => Promise<void>, fallbackMessage: string) {
    try {
      await action();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, fallbackMessage), type: "error" });
      if (isSessionExpired(error)) setStep(START);
    }
  }

  return {
    step,
    goTo: setStep,
    backToStart: () => setStep(START),

    submitCredentials: ({ email, password }: CredentialsValues) =>
      attempt(async () => {
        const { challengeToken, resendInSeconds } = await authService.login(email, password);
        setStep({ name: "OTP", challengeToken, resendInSeconds });
      }, "Email ou mot de passe incorrect."),

    submitForgotPassword: ({ email }: ForgotPasswordValues) =>
      attempt(async () => {
        await authService.forgotPassword(email);
        setStep({ name: "FORGOT_PASSWORD_SENT" });
      }, "Impossible d'envoyer le lien de réinitialisation."),

    /** @returns the cooldown to wait before the next resend, or null if the resend failed. */
    resendOtp: async (challengeToken: string): Promise<number | null> => {
      let seconds: number | null = null;
      await attempt(async () => {
        seconds = (await authService.resendOtp(challengeToken)).resendInSeconds;
        addToast({ title: "Code renvoyé", description: "Un nouveau code a été envoyé par email.", type: "success" });
      }, "Impossible de renvoyer le code.");
      return seconds;
    },

    submitOtp: (challengeToken: string, otp: string) =>
      attempt(async () => {
        const { nextStep } = await authService.verifyOtp(challengeToken, otp);
        if (nextStep === "MFA_SETUP") {
          const setup = await authService.beginMfaSetup(challengeToken);
          setStep({ name: "MFA_SETUP", challengeToken, setup });
        } else {
          setStep({ name: "TOTP", challengeToken });
        }
      }, "Code incorrect ou expiré."),

    submitTotp: (challengeToken: string, code: string) =>
      attempt(async () => {
        await authService.verifyTotp(challengeToken, code);
        addToast({ title: "Succès", description: "Connexion sécurisée réussie.", type: "success" });
        router.replace("/");
      }, "Code d'authentification incorrect."),
  };
}
