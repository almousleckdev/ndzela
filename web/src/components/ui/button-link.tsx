import Link from "next/link";
import { cn } from "@/lib/utils";

const VARIANTS = {
  primary: "bg-action text-white hover:bg-action-hover",
  secondary: "border border-ink/15 bg-white text-ink hover:bg-sky",
} as const;

interface ButtonLinkProps extends React.ComponentProps<typeof Link> {
  variant?: keyof typeof VARIANTS;
}

// A link that looks like a button
export function ButtonLink({ variant = "primary", className, ...props }: ButtonLinkProps) {
  return (
    <Link
      className={cn("inline-flex h-11 items-center justify-center rounded-lg px-5 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/40", VARIANTS[variant], className)}
      {...props}
    />
  );
}
