import * as React from "react";
import { cn } from "@/lib/utils";

interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "type"> {
  label: string;
  description?: string;
}

export const Checkbox = React.forwardRef<HTMLInputElement, CheckboxProps>(
  ({ label, description, className, ...props }, ref) => (
    <label className={cn("flex cursor-pointer items-start gap-3 rounded-md p-2 hover:bg-slate-50", props.disabled && "cursor-not-allowed opacity-60", className)}>
      <input ref={ref} type="checkbox" className="mt-0.5 h-4 w-4 rounded border-slate-300 accent-primary" {...props} />
      <span>
        <span className="block text-sm font-medium text-slate-800">{label}</span>
        {description && <span className="block text-xs text-slate-500">{description}</span>}
      </span>
    </label>
  )
);

Checkbox.displayName = "Checkbox";
