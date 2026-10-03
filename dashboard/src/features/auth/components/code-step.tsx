"use client";

import type { LucideIcon } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { useCountdown } from "@/features/auth/hooks/use-countdown";
import { CodeInput } from "@/components/ui/code-input";
import { otpSchema, type CodeValues } from "@/features/auth/schemas/auth";
import { StepHeader } from "@/features/auth/components/step-header";

interface CodeStepProps {
  icon: LucideIcon;
  title: string;
  description: string;
  submitLabel: string;
  onSubmit: (code: string) => Promise<void>;
  // Shows a "resend" button that is locked for `initialSeconds`, then again after each resend.
  resend?: { initialSeconds: number; onResend: () => Promise<number | null> };
}

// One six-digit code screen, used for both the emailed OTP and the authenticator (TOTP) code.
export function CodeStep({ icon, title, description, submitLabel, onSubmit, resend }: CodeStepProps) {
  const { remaining, restart } = useCountdown(resend?.initialSeconds ?? 0);
  const { register, handleSubmit, formState } = useForm<CodeValues>({ resolver: zodResolver(otpSchema) });

  return (
    <form onSubmit={handleSubmit(({ code }) => onSubmit(code))} className="space-y-6" noValidate>
      <StepHeader icon={icon} title={title} description={description} />
      <CodeInput autoFocus error={formState.errors.code?.message} {...register("code")} />
      <Button type="submit" size="xl" className="w-full" isLoading={formState.isSubmitting}>
        {submitLabel}
      </Button>
      {resend && (
        <Button
          variant="ghost"
          disabled={remaining > 0}
          onClick={async () => {
            const seconds = await resend.onResend();
            if (seconds !== null) restart(seconds);
          }}
          className="w-full font-semibold text-slate-600"
        >
          {remaining > 0 ? `Renvoyer le code dans ${remaining} s` : "Renvoyer le code"}
        </Button>
      )}
    </form>
  );
}
