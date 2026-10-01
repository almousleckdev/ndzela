import * as React from "react";
import { Label } from "@/components/ui/label";
import { Select, type SelectProps } from "@/components/ui/select";

interface SelectFieldProps extends SelectProps {
  label: string;
  error?: string;
  options: { value: string; label: string }[];
  placeholder?: string;
}

/** Label + select + error message, wired together for accessibility. */
const SelectField = React.forwardRef<HTMLSelectElement, SelectFieldProps>(({ label, error, options, placeholder, id, ...props }, ref) => {
  const generatedId = React.useId();
  const fieldId = id ?? generatedId;
  return (
    <div>
      <Label htmlFor={fieldId}>{label}</Label>
      <Select ref={ref} id={fieldId} aria-invalid={error ? true : undefined} aria-describedby={error ? `${fieldId}-message` : undefined} {...props}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
      </Select>
      {error && <p id={`${fieldId}-message`} role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
    </div>
  );
});
SelectField.displayName = "SelectField";

export { SelectField };
