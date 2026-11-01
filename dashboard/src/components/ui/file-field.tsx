"use client";

import { useEffect, useId, useMemo, useRef } from "react";
import { FileText, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

export const MAX_FILE_BYTES = 5 * 1024 * 1024;
export const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
export const DOCUMENT_TYPES = [...IMAGE_TYPES, "application/pdf"];

// The message to show when the file is unusable, or undefined when it is fine.
export function fileProblem(file: File, accepted: string[]): string | undefined {
  if (!accepted.includes(file.type)) return accepted.includes("application/pdf") ? "Choisissez une image (JPG, PNG) ou un PDF." : "Choisissez une image (JPG, PNG).";
  if (file.size > MAX_FILE_BYTES) return "Le fichier dépasse 5 Mo.";
  return undefined;
}

interface FileFieldProps {
  label: string;
  hint?: string;
  accepted: string[];
  file: File | null;
  error?: string;
  onChange: (file: File | null, error?: string) => void;
}

// Picks one image (or PDF) and previews it; the type and size are checked here for convenience
export function FileField({ label, hint, accepted, file, error, onChange }: FileFieldProps) {
  const id = useId();
  const input = useRef<HTMLInputElement>(null);
  const preview = useMemo(() => (file && file.type.startsWith("image/") ? URL.createObjectURL(file) : null), [file]);
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);

  const pick = (chosen: File | undefined) => {
    if (!chosen) return;
    onChange(fileProblem(chosen, accepted) ? null : chosen, fileProblem(chosen, accepted));
  };

  return (
    <div>
      <Label htmlFor={id}>{label}</Label>
      <div className="flex items-center gap-3 rounded-lg border border-dashed border-slate-300 p-3">
        {preview ? (
          <a href={preview} target="_blank" rel="noopener noreferrer" title="Voir en grand">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={preview} alt="" className="h-20 w-20 rounded-md border border-slate-200 object-cover" />
          </a>
        ) : (
          <span className="flex h-20 w-20 items-center justify-center rounded-md bg-slate-100 text-slate-500">{file ? <FileText className="h-6 w-6" /> : <Upload className="h-6 w-6" />}</span>
        )}
        <div className="min-w-0 flex-1 text-sm">
          <p className="truncate font-medium text-slate-900">{file ? file.name : "Aucun fichier"}</p>
          <p className="text-xs text-slate-500">{hint ?? "JPG, PNG ou PDF, 5 Mo maximum."}</p>
        </div>
        <input ref={input} id={id} type="file" accept={accepted.join(",")} className="hidden" onChange={(event) => { pick(event.target.files?.[0]); event.target.value = ""; }} />
        <Button variant="outline" size="sm" onClick={() => input.current?.click()}>{file ? "Changer" : "Choisir"}</Button>
        {file && <Button variant="ghost" size="icon" aria-label="Retirer le fichier" onClick={() => onChange(null)}><X className="h-4 w-4" /></Button>}
      </div>
      {error && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
    </div>
  );
}
