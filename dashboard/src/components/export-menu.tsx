"use client";

import { useState } from "react";
import { ChevronDown, Download, FileSpreadsheet, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { useToast } from "@/hooks/use-toast";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";

export type ExportFormat = "XLSX" | "PDF";

interface ExportMenuProps {
  // The export endpoint for the chosen format, with the filters of the page
  path: (format: ExportFormat) => string;
  // The file name without its extension
  filename: string;
  failure: string;
}

// "Exporter" button: the user picks Excel or PDF.
export function ExportMenu({ path, filename, failure }: ExportMenuProps) {
  const { addToast } = useToast();
  const [isExporting, setIsExporting] = useState(false);

  const run = async (format: ExportFormat) => {
    setIsExporting(true);
    try {
      await downloadFile(path(format), `${filename}.${format === "PDF" ? "pdf" : "xlsx"}`);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, failure), type: "error" });
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <Dropdown
      trigger={<Button variant="outline" isLoading={isExporting}><Download className="mr-2 h-4 w-4" /> Exporter <ChevronDown className="ml-1 h-4 w-4" /></Button>}
    >
      <DropdownItem onClick={() => run("XLSX")}><span className="flex items-center"><FileSpreadsheet className="mr-2 h-4 w-4 text-emerald-600" /> Excel (.xlsx)</span></DropdownItem>
      <DropdownItem onClick={() => run("PDF")}><span className="flex items-center"><FileText className="mr-2 h-4 w-4 text-red-600" /> PDF</span></DropdownItem>
    </Dropdown>
  );
}
