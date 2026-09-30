import { Search } from "lucide-react";
import { cn } from "@/lib/utils";
import { Input, type InputProps } from "@/components/ui/input";

/** Rounded search field with a leading icon. `label` is read by screen readers. */
export function SearchInput({ label, className, ...props }: InputProps & { label: string }) {
  return (
    <div className={cn("relative", className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <Input type="search" aria-label={label} className="rounded-full bg-slate-50 pl-9 focus-visible:bg-white" {...props} />
    </div>
  );
}
