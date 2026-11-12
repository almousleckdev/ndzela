"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { SelectField } from "@/components/ui/select-field";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs } from "@/components/ui/tabs";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatDateTime } from "@/lib/time";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { HalfDaysPanel } from "@/features/payments/components/half-days-panel";
import { LEAVE_REASON_LABELS, toOptions } from "@/features/drivers/lib/driver-labels";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver, Leave, LeaveReason, LeaveType } from "@/features/drivers/types";

const STATE_LABELS = { UPCOMING: "À venir", ONGOING: "En cours", PAST: "Terminé", CANCELLED: "Annulé" } as const;
const STATE_VARIANTS = { UPCOMING: "info", ONGOING: "warning", PAST: "neutral", CANCELLED: "neutral" } as const;

const localToday = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
};
const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR");
const period = (leave: Leave) => (leave.startDate === leave.endDate ? day(leave.startDate) : `${day(leave.startDate)} au ${day(leave.endDate)}`);

interface DriverLeavesDialogProps {
  driver: Driver;
  canDeclare: boolean;
  initialTab?: "leaves" | "half-days";
  onClose: () => void;
  // Called after a declaration or a cancellation, so the caller can refresh the driver's status.
  onChanged: () => void;
}

// Declares days off (single day or date range) and lists the driver's leave history.
export function DriverLeavesDialog({ driver, canDeclare, initialTab = "leaves", onClose, onChanged }: DriverLeavesDialogProps) {
  const { addToast } = useToast();
  const history = useAsync(() => driversService.leaves(driver.id), [driver.id]);
  const { can } = usePermissions();
  const [tab, setTab] = useState<"leaves" | "half-days">(initialTab);
  const showHalfDays = Boolean(driver.contract) && can(PERMISSIONS.PAYMENTS_READ);
  const [type, setType] = useState<LeaveType>("SINGLE_DAY");
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [reason, setReason] = useState<LeaveReason | "">("");
  const [note, setNote] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const today = localToday();

  const declare = async () => {
    if (!start || (type === "DATE_RANGE" && !end)) return setError("Indiquez la date.");
    if (start < today) return setError("Une date passée ne peut pas être déclarée.");
    if (type === "DATE_RANGE" && end < start) return setError("La date de fin doit suivre la date de début.");
    if (!reason) return setError("Choisissez un motif.");
    if (reason === "OTHER" && !note.trim()) return setError("Précisez le motif.");
    setIsSaving(true);
    try {
      await driversService.declareLeave(driver.id, { type, startDate: start, endDate: type === "DATE_RANGE" ? end : undefined, reason, note: note.trim() || undefined });
      addToast({ title: "Succès", description: "Arrêt déclaré.", type: "success" });
      setStart(""); setEnd(""); setReason(""); setNote(""); setError(undefined);
      history.reload();
      onChanged();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible de déclarer l'arrêt."));
    } finally {
      setIsSaving(false);
    }
  };

  const cancel = async (leave: Leave) => {
    try {
      await driversService.cancelLeave(driver.id, leave.id);
      addToast({ title: "Succès", description: "Arrêt annulé.", type: "success" });
      history.reload();
      onChanged();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible d'annuler l'arrêt."), type: "error" });
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Gérer les arrêts" description={`${driver.personal.lastName} ${driver.personal.firstName}. Le loyer n'est pas dû les jours d'arrêt déclarés.`} className="max-w-2xl">
      {showHalfDays && <Tabs tabs={[{ value: "leaves", label: "Arrêts" }, { value: "half-days", label: "Demi-journées" }]} value={tab} onChange={setTab} label="Arrêts et demi-journées" />}
      {tab === "half-days" && showHalfDays ? (
        <div className="pt-4"><HalfDaysPanel driverId={driver.id} driverName={`${driver.personal.lastName} ${driver.personal.firstName}`} contractStart={driver.contract?.startDate} canDeclare={can(PERMISSIONS.INCIDENTS_DECLARE)} onChanged={onChanged} /></div>
      ) : (
      <div className="space-y-6">
        {canDeclare && (
          <section className="space-y-4">
            <h3 className="text-sm font-semibold text-slate-900">Déclarer un arrêt</h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <SelectField label="Type *" options={[{ value: "SINGLE_DAY", label: "Jour unique" }, { value: "DATE_RANGE", label: "Plage de dates" }]} value={type} onChange={(event) => setType(event.target.value as LeaveType)} />
              <SelectField label="Motif *" placeholder="Choisir" options={toOptions(LEAVE_REASON_LABELS)} value={reason} onChange={(event) => setReason(event.target.value as LeaveReason)} />
              <FormField label={type === "SINGLE_DAY" ? "Date *" : "Du *"} type="date" min={today} value={start} onChange={(event) => setStart(event.target.value)} />
              {type === "DATE_RANGE" && <FormField label="Au *" type="date" min={start || today} value={end} onChange={(event) => setEnd(event.target.value)} />}
            </div>
            <div>
              <Label htmlFor="leave-note">{reason === "OTHER" ? "Précisez le motif *" : "Note"}</Label>
              <textarea id="leave-note" rows={2} maxLength={255} value={note} onChange={(event) => setNote(event.target.value)}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
            </div>
            {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
            <div className="flex justify-end"><Button onClick={declare} isLoading={isSaving}>Déclarer l&apos;arrêt</Button></div>
          </section>
        )}

        <section>
          <h3 className="mb-2 text-sm font-semibold text-slate-900">Historique des arrêts</h3>
          {!history.data ? <Skeleton className="h-16 w-full" /> : history.data.length === 0 ? (
            <p className="text-sm text-slate-500">Aucun arrêt déclaré.</p>
          ) : (
            <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
              {history.data.map((leave) => (
                <li key={leave.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2.5 text-sm">
                  <div>
                    <p className="font-medium text-slate-900">{period(leave)} · {LEAVE_REASON_LABELS[leave.reason]}</p>
                    <p className="text-xs text-slate-500">{leave.note ? `${leave.note} · ` : ""}Déclaré par {leave.declaredBy} le {formatDateTime(leave.createdAt)}{leave.cancelledBy ? `, annulé par ${leave.cancelledBy}` : ""}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={STATE_VARIANTS[leave.state]}>{STATE_LABELS[leave.state]}</Badge>
                    {canDeclare && leave.state === "UPCOMING" && <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" onClick={() => cancel(leave)}>Annuler</Button>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
      )}
    </Dialog>
  );
}
