import * as React from "react";
import { cn } from "@/lib/utils";
import { Input, type InputProps } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

export interface PhoneCodeOption {
  value: string;
  label: string;
}

export interface PhoneFieldProps extends InputProps {
  label: string;
  error?: string;
  codes: PhoneCodeOption[];
  code: string;
  onCodeChange: (code: string) => void;
}

/** A country-code dropdown next to the number input. */
const PhoneField = React.forwardRef<HTMLInputElement, PhoneFieldProps>(
  ({ label, error, codes, code, onCodeChange, id, className, ...props }, ref) => {
    const generatedId = React.useId();
    const fieldId = id ?? generatedId;
    const messageId = `${fieldId}-message`;

    return (
      <div>
        <Label htmlFor={fieldId}>{label}</Label>
        <div className="flex gap-2">
          <Select aria-label="Indicatif du pays" className="w-36 shrink-0" value={code} onChange={(event) => onCodeChange(event.target.value)}>
            {codes.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </Select>
          <Input
            ref={ref}
            id={fieldId}
            type="tel"
            inputMode="numeric"
            aria-invalid={error ? true : undefined}
            aria-describedby={error ? messageId : undefined}
            className={cn(className)}
            {...props}
          />
        </div>
        {error && <p id={messageId} role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
      </div>
    );
  }
);
PhoneField.displayName = "PhoneField";

export { PhoneField };
