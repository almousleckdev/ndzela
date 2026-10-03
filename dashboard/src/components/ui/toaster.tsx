"use client";

import { useToast } from "@/hooks/use-toast";
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

export function Toaster() {
  const { toasts, removeToast } = useToast();

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => {
        const icons = {
          success: <CheckCircle className="h-5 w-5 text-green-500" />,
          error: <AlertCircle className="h-5 w-5 text-red-500" />,
          warning: <AlertTriangle className="h-5 w-5 text-yellow-500" />,
          info: <Info className="h-5 w-5 text-blue-500" />,
        };

        const icon = icons[toast.type || 'info'];

        return (
          <div
            key={toast.id}
            className={cn(
              "flex w-80 items-start gap-3 rounded-lg border bg-white p-4 shadow-lg transition-all duration-300",
              toast.type === 'error' && "border-red-100",
              toast.type === 'success' && "border-green-100"
            )}
          >
            {icon}
            <div className="flex-1">
              <h4 className="text-sm font-semibold text-slate-900">{toast.title}</h4>
              {toast.description && (
                <p className="mt-1 text-sm text-slate-500">{toast.description}</p>
              )}
            </div>
            <Button variant="ghost" size="icon" className="h-6 w-6" aria-label="Fermer" onClick={() => removeToast(toast.id)}>
              <X className="h-4 w-4 text-slate-400" />
            </Button>
          </div>
        );
      })}
    </div>
  );
}
