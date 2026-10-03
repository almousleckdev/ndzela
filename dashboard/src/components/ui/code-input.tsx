import * as React from "react";
import { cn } from "@/lib/utils";
import { Input, type InputProps } from "@/components/ui/input";

// Six-digit one-time code field (email OTP and authenticator code).
const CodeInput = React.forwardRef<HTMLInputElement, InputProps & { error?: string }>(
  ({ className, error, ...props }, ref) => (
    <div>
      <Input
        ref={ref}
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        placeholder="000000"
        aria-label="Code à 6 chiffres"
        aria-invalid={error ? true : undefined}
        className={cn("h-14 bg-white text-center font-mono text-2xl tracking-[0.5em]", className)}
        {...props}
      />
      {error && <p role="alert" className="mt-1 text-center text-xs font-medium text-red-600">{error}</p>}
    </div>
  )
);
CodeInput.displayName = "CodeInput";

export { CodeInput };
