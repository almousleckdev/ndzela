import * as React from "react";
import { cn } from "@/lib/utils";

interface LabelProps extends React.LabelHTMLAttributes<HTMLLabelElement> {
  // Render as a to caption a (groups of checkboxes).
  asLegend?: boolean;
}

export function Label({ className, asLegend, ...props }: LabelProps) {
  const classes = cn("mb-1.5 block text-sm font-medium text-slate-800", className);
  return asLegend ? <legend className={classes}>{props.children}</legend> : <label className={classes} {...props} />;
}
