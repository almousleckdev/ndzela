"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

interface PopoverProps {
  /** The always-visible button content; the popover wraps it in a button. */
  trigger: React.ReactNode;
  /** The panel; receives a function that closes it. */
  children: (close: () => void) => React.ReactNode;
  align?: "start" | "end";
  label: string;
  triggerClassName?: string;
  panelClassName?: string;
}

/** A button that opens a floating panel: closes on outside click and Escape, announces its state. */
export function Popover({ trigger, children, align = "start", label, triggerClassName, panelClassName }: PopoverProps) {
  const [open, setOpen] = React.useState(false);
  const root = React.useRef<HTMLDivElement>(null);
  const close = React.useCallback(() => setOpen(false), []);

  React.useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false); };
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") setOpen(false); };
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => { document.removeEventListener("pointerdown", onPointer); document.removeEventListener("keydown", onKey); };
  }, [open]);

  return (
    <div ref={root} className="relative">
      <button type="button" aria-haspopup="dialog" aria-expanded={open} aria-label={label} onClick={() => setOpen((value) => !value)}
        className={cn("inline-flex h-9 items-center gap-2 rounded-md border border-line bg-white px-3 text-sm font-medium text-ink transition-colors hover:border-slate-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary", open && "border-slate-300 bg-slate-50", triggerClassName)}>
        {trigger}
      </button>
      {open && (
        <div role="dialog" aria-label={label} className={cn("absolute top-full z-30 mt-1.5 rounded-lg border border-line bg-white shadow-lg", align === "end" ? "right-0" : "left-0", panelClassName)}>
          {children(close)}
        </div>
      )}
    </div>
  );
}
