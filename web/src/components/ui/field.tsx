import { useId } from "react";
import { cn } from "@/lib/utils";

interface FieldShellProps {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
  id: string;
  children: React.ReactNode;
}

function FieldShell({ label, error, hint, required, id, children }: FieldShellProps) {
  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold">{label}{required && <span className="text-action"> *</span>}</label>
      {children}
      {hint && !error && <p id={`${id}-message`} className="mt-1.5 text-sm text-muted">{hint}</p>}
      {error && <p id={`${id}-message`} role="alert" className="mt-1.5 text-sm font-medium text-red-700">{error}</p>}
    </div>
  );
}

const CONTROL = "w-full rounded-lg border border-ink/20 bg-white px-3.5 text-base text-ink placeholder:text-muted/70 focus-visible:border-action focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/30 aria-[invalid=true]:border-red-600";

interface InputFieldProps extends React.ComponentProps<"input"> {
  label: string;
  error?: string;
  hint?: string;
}

export function InputField({ label, error, hint, className, required, ...props }: InputFieldProps) {
  const id = useId();
  return (
    <FieldShell label={label} error={error} hint={hint} required={required} id={id}>
      <input id={id} aria-invalid={error ? true : undefined} aria-describedby={error || hint ? `${id}-message` : undefined} className={cn(CONTROL, "h-11", className)} {...props} />
    </FieldShell>
  );
}

interface TextareaFieldProps extends React.ComponentProps<"textarea"> {
  label: string;
  error?: string;
  hint?: string;
}

export function TextareaField({ label, error, hint, className, required, ...props }: TextareaFieldProps) {
  const id = useId();
  return (
    <FieldShell label={label} error={error} hint={hint} required={required} id={id}>
      <textarea id={id} aria-invalid={error ? true : undefined} aria-describedby={error || hint ? `${id}-message` : undefined} className={cn(CONTROL, "min-h-32 py-3", className)} {...props} />
    </FieldShell>
  );
}
