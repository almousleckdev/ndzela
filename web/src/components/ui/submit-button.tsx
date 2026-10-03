import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface SubmitButtonProps extends React.ComponentProps<"button"> {
  loading?: boolean;
}

export function SubmitButton({ loading, className, children, disabled, ...props }: SubmitButtonProps) {
  return (
    <button type="submit" disabled={disabled || loading} className={cn("inline-flex h-12 items-center justify-center gap-2 rounded-lg bg-action px-6 text-base font-semibold text-white transition-colors hover:bg-action-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/40 disabled:cursor-not-allowed disabled:opacity-60", className)} {...props}>
      {loading && <Loader2 className="h-4 w-4 animate-spin" aria-hidden />}
      {children}
    </button>
  );
}
