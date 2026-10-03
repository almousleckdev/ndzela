import type { LucideIcon } from "lucide-react";

export function StepHeader({ icon: Icon, title, description }: { icon?: LucideIcon; title: string; description: string }) {
  return (
    <div className="mb-6 text-center">
      {Icon && <Icon className="mx-auto mb-3 h-8 w-8 text-primary" />}
      <h1 className="text-lg font-bold text-slate-900">{title}</h1>
      <p className="mt-1 text-sm font-medium text-slate-700">{description}</p>
    </div>
  );
}
