import * as React from "react";
import { Eye, EyeOff, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input, type InputProps } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export interface FormFieldProps extends InputProps {
  label: string;
  error?: string;
  hint?: string;
  icon?: LucideIcon;
}

/** Label + input (+ optional leading icon) + hint/error message, wired together for accessibility. */
const FormField = React.forwardRef<HTMLInputElement, FormFieldProps>(
  ({ label, error, hint, icon: Icon, className, id, type, ...props }, ref) => {
    const [isRevealed, setIsRevealed] = React.useState(false);
    const isPassword = type === "password";
    const generatedId = React.useId();
    const fieldId = id ?? generatedId;
    const messageId = `${fieldId}-message`;

    return (
      <div>
        <Label htmlFor={fieldId}>{label}</Label>
        <div className="relative">
          {Icon && <Icon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />}
          <Input
            ref={ref}
            id={fieldId}
            aria-invalid={error ? true : undefined}
            aria-describedby={error || hint ? messageId : undefined}
            type={isPassword && isRevealed ? "text" : type}
            className={cn(Icon && "pl-9", isPassword && "pr-10", className)}
            {...props}
          />
          {isPassword && (
            <Button
              variant="ghost"
              size="icon"
              className="absolute right-1 top-1/2 h-8 w-8 -translate-y-1/2 text-slate-500"
              aria-label={isRevealed ? "Masquer le mot de passe" : "Afficher le mot de passe"}
              aria-pressed={isRevealed}
              onClick={() => setIsRevealed((value) => !value)}
            >
              {isRevealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </Button>
          )}
        </div>
        {error ? (
          <p id={messageId} role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>
        ) : hint ? (
          <p id={messageId} className="mt-1 text-xs text-slate-500">{hint}</p>
        ) : null}
      </div>
    );
  }
);
FormField.displayName = "FormField";

export { FormField };
