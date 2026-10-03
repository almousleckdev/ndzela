"use client";

import { useId, useRef } from "react";
import { FileText, ImageIcon, X } from "lucide-react";

interface FileFieldProps {
  label: string;
  hint?: string;
  accept: string;
  file: File | null;
  error?: string;
  required?: boolean;
  onChange: (file: File | null) => void;
}

function size(bytes: number): string {
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} Mo` : `${Math.max(1, Math.round(bytes / 1024))} Ko`;
}

// One file to attach
export function FileField({ label, hint, accept, file, error, required, onChange }: FileFieldProps) {
  const id = useId();
  const input = useRef<HTMLInputElement>(null);
  const Icon = file?.type.startsWith("image/") ? ImageIcon : FileText;

  return (
    <div>
      <label htmlFor={id} className="mb-1.5 block text-sm font-semibold">{label}{required && <span className="text-action"> *</span>}</label>
      <input ref={input} id={id} type="file" accept={accept} className="sr-only" aria-describedby={`${id}-message`}
        onChange={(event) => { onChange(event.target.files?.[0] ?? null); event.target.value = ""; }} />
      {file ? (
        <div className="flex items-center gap-3 rounded-lg border border-ink/20 bg-sky px-3.5 py-2.5">
          <Icon className="h-5 w-5 shrink-0 text-action" aria-hidden />
          <span className="min-w-0 flex-1 truncate text-sm font-medium">{file.name}<span className="ml-2 font-normal text-muted">{size(file.size)}</span></span>
          <button type="button" onClick={() => onChange(null)} aria-label={`Retirer ${file.name}`} className="rounded p-1 text-muted hover:bg-white hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/40"><X className="h-4 w-4" /></button>
        </div>
      ) : (
        <button type="button" onClick={() => input.current?.click()} data-invalid={error ? "true" : undefined}
          className="flex h-11 w-full items-center justify-center rounded-lg border border-dashed border-ink/30 text-sm font-medium text-muted transition-colors hover:border-action hover:text-action focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action/30 data-[invalid=true]:border-red-600">
          Choisir un fichier
        </button>
      )}
      {hint && !error && <p id={`${id}-message`} className="mt-1.5 text-sm text-muted">{hint}</p>}
      {error && <p id={`${id}-message`} role="alert" className="mt-1.5 text-sm font-medium text-red-700">{error}</p>}
    </div>
  );
}
