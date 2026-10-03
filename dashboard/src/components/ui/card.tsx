import * as React from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-xl border border-slate-200 bg-white p-6 shadow-sm", className)} {...props} />;
}

export function CardTitle({ icon: Icon, children, className }: { icon?: LucideIcon; children: React.ReactNode; className?: string }) {
  return (
    <div className={cn("mb-4 flex items-center gap-3", className)}>
      {Icon && <Icon className="h-5 w-5 text-slate-400" />}
      <h3 className="text-lg font-semibold text-slate-900">{children}</h3>
    </div>
  );
}
