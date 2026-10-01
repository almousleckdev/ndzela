"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { useToast } from "@/hooks/use-toast";
import { downloadFile } from "@/lib/download";
import { getErrorMessage } from "@/lib/errors";
import { chargingService } from "@/features/charging/services/charging.service";
import type { ChargingPeriod } from "@/features/charging/types";

const PERIODS: { value: ChargingPeriod; label: string; file: string }[] = [
  { value: "DAY", label: "Jour", file: "jour" },
  { value: "WEEK", label: "Semaine (lundi au dimanche)", file: "semaine" },
  { value: "MONTH", label: "Mois", file: "mois" },
  { value: "YEAR", label: "Année", file: "annee" },
];

/** Downloads the recharges of a day, a week, a month or a year as a CSV with totals. */
export function ExportDialog({ day, onClose }: { day: string; onClose: () => void }) {
  const { addToast } = useToast();
  const [period, setPeriod] = useState<ChargingPeriod>("DAY");
  const [date, setDate] = useState(day);
  const [isExporting, setIsExporting] = useState(false);

  const download = async () => {
    setIsExporting(true);
    try {
      await downloadFile(chargingService.exportPath(period, date), `recharges-${PERIODS.find((p) => p.value === period)?.file}-${date}.csv`);
      onClose();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'exporter les recharges."), type: "error" });
      setIsExporting(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Exporter les recharges" description="Le fichier contient le détail et le total de la période qui contient la date choisie.">
      <div className="space-y-4">
        <SelectField label="Période *" options={PERIODS.map(({ value, label }) => ({ value, label }))} value={period} onChange={(event) => setPeriod(event.target.value as ChargingPeriod)} />
        <FormField label="Date de référence *" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isExporting}>Annuler</Button>
          <Button onClick={download} isLoading={isExporting} disabled={!date}>Exporter</Button>
        </div>
      </div>
    </Dialog>
  );
}
