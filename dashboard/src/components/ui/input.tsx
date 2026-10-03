import * as React from "react";
import { cn } from "@/lib/utils";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  // "glass" is the translucent style used on the auth screens.
  variant?: "default" | "glass";
}

const variants = {
  default: "h-9 border-slate-200 bg-transparent",
  glass: "h-11 border-white/50 bg-white/70 text-slate-900 shadow-sm focus:bg-white",
};

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, variant = "default", ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "flex w-full rounded-md border px-3 py-1 text-sm shadow-sm transition-colors placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50",
        variants[variant],
        className
      )}
      {...props}
    />
  )
);
Input.displayName = "Input";

export { Input };
