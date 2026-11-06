"use client";

import { useState } from "react";
import { ChevronDown, Download, FileSpreadsheet, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { ExportPeriod } from "@/components/export-period";
import { useToast } from "@/hooks/use-toast";
import { describeRange, type DateRange } from "@/lib/date-range";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";
import { cn } from "@/lib/utils";

export type ExportFormat = "XLSX" | "PDF";

// What the user chose in the export window
export interface ExportChoice {
  range: DateRange;
  option?: string;
}

interface ExportMenuProps {
  // The export endpoint for the chosen format, the period and the option
  path: (format: ExportFormat, choice: ExportChoice) => string;
  // The file name without its extension
  filename: string;
  failure: string;
  // The period the page shows: when given, the export window asks for the period, starting from it
  range?: DateRange;
  // A second choice that narrows the export (for example the kind of sessions)
  option?: { label: string; options: { value: string; label: string }[]; initial: string };
}

// "Exporter" button: Excel or PDF straight away, or a window to choose the period and what to include first.
export function ExportMenu({ path, filename, failure, range, option }: ExportMenuProps) {
  const { addToast } = useToast();
  const [isExporting, setIsExporting] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [period, setPeriod] = useState<DateRange | undefined>(range);
  const [choice, setChoice] = useState(option?.initial);

  const run = async (format: ExportFormat, picked: ExportChoice) => {
    setIsExporting(true);
    try {
      const dates = picked.range.from || picked.range.to ? `-${picked.range.from || "debut"}_${picked.range.to || "aujourdhui"}` : "";
      await downloadFile(path(format, picked), `${filename}${dates}.${format === "PDF" ? "pdf" : "xlsx"}`);
      setIsOpen(false);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, failure), type: "error" });
    } finally {
      setIsExporting(false);
    }
  };

  const trigger = <Button variant="outline" isLoading={isExporting}><Download className="mr-2 h-4 w-4" /> Exporter <ChevronDown className="ml-1 h-4 w-4" /></Button>;
  if (!range) {
    return (
      <Dropdown trigger={trigger}>
        <DropdownItem onClick={() => run("XLSX", { range: { from: "", to: "", preset: "all" } })}><span className="flex items-center"><FileSpreadsheet className="mr-2 h-4 w-4 text-emerald-600" /> Excel (.xlsx)</span></DropdownItem>
        <DropdownItem onClick={() => run("PDF", { range: { from: "", to: "", preset: "all" } })}><span className="flex items-center"><FileText className="mr-2 h-4 w-4 text-red-600" /> PDF</span></DropdownItem>
      </Dropdown>
    );
  }

  const picked: ExportChoice = { range: period ?? range, option: choice };
  return (
    <>
      <Button variant="outline" onClick={() => { setPeriod(range); setChoice(option?.initial); setIsOpen(true); }}><Download className="mr-2 h-4 w-4" /> Exporter</Button>
      <Dialog isOpen={isOpen} onClose={() => setIsOpen(false)} title="Exporter" description="Choisissez la période et le contenu, puis le format." className="max-w-xl">
        <div className="space-y-5">
          <section><h4 className="mb-2 text-sm font-semibold text-slate-900">Période</h4><ExportPeriod value={picked.range} onChange={setPeriod} /></section>
          {option && (
            <section>
              <h4 className="mb-2 text-sm font-semibold text-slate-900">{option.label}</h4>
              <div className="flex flex-wrap gap-2" role="group" aria-label={option.label}>
                {option.options.map((o) => (
                  <button key={o.value} type="button" aria-pressed={choice === o.value} onClick={() => setChoice(o.value)}
                    className={cn("rounded-full border px-3 py-1.5 text-sm", choice === o.value ? "border-primary bg-primary/10 font-medium text-primary-ink" : "border-slate-200 text-slate-700 hover:bg-slate-50")}>
                    {o.label}
                  </button>
                ))}
              </div>
            </section>
          )}
          <p className="text-sm text-slate-500">Période exportée : <span className="font-medium text-slate-900">{describeRange(picked.range)}</span></p>
          <div className="flex flex-wrap justify-end gap-2">
            <Button variant="outline" isLoading={isExporting} onClick={() => run("XLSX", picked)}><FileSpreadsheet className="mr-2 h-4 w-4 text-emerald-600" /> Excel (.xlsx)</Button>
            <Button isLoading={isExporting} onClick={() => run("PDF", picked)}><FileText className="mr-2 h-4 w-4" /> PDF</Button>
          </div>
        </div>
      </Dialog>
    </>
  );
}
