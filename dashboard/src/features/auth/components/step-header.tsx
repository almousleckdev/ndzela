import type { LucideIcon } from "lucide-react";

export function StepHeader({ icon: Icon, title, description }: { icon?: LucideIcon; title: string; description: string }) {
  return (
    <div className="mb-8">
      {Icon && <Icon className="mb-3 h-7 w-7 text-primary" />}
      <h1 className="text-2xl font-semibold text-slate-900">{title}</h1>
      <p className="mt-1.5 text-sm text-slate-500">{description}</p>
    </div>
  );
}
