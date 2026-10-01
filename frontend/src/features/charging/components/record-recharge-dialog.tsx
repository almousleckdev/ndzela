"use client";

import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { useFetch } from "@/hooks/use-fetch";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { chargingService } from "@/features/charging/services/charging.service";
import { METHOD_LABELS } from "@/features/payments/lib/payment-labels";
import type { PaymentMethod } from "@/features/payments/types";

/** Records a recharge paid at the station. The request id is created once per dialog, so a double click records it once. */
export function RecordRechargeDialog({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const { addToast } = useToast();
  const requestId = useRef(crypto.randomUUID());
  const vehicles = useFetch(() => chargingService.vehicles());
  const stations = useFetch(chargingService.stations);
  const [kind, setKind] = useState<"OWN" | "PUBLIC">("OWN");
  const [vehicleId, setVehicleId] = useState("");
  const [publicLabel, setPublicLabel] = useState("");
  const [stationId, setStationId] = useState("");
  const [amount, setAmount] = useState("");
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [reference, setReference] = useState("");
  const [before, setBefore] = useState("");
  const [after, setAfter] = useState("");
  const [kwh, setKwh] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const chosen = vehicles.data?.find((option) => option.id === vehicleId);
  const touch = <T,>(setter: (value: T) => void) => (value: T) => { setter(value); setError(undefined); };

  const save = async () => {
    const b = Number(before);
    const a = Number(after);
    if (kind === "OWN" && !vehicleId) return setError("Choisissez le véhicule.");
    if (kind === "PUBLIC" && !publicLabel.trim()) return setError("Saisissez la plaque ou la description de la voiture.");
    if (!stationId) return setError("Choisissez la station.");
    if (!/^\d{1,14}$/.test(amount) || Number(amount) < 1) return setError("Saisissez un montant entier supérieur à 0.");
    if (method !== "CASH" && !reference.trim()) return setError("La référence est obligatoire hors espèces.");
    if (!/^\d{1,3}$/.test(before) || !/^\d{1,3}$/.test(after) || b > 100 || a > 100) return setError("Les niveaux de batterie sont des pourcentages entre 0 et 100.");
    if (a < b) return setError("Le niveau après la recharge ne peut pas être inférieur au niveau avant.");
    if (kwh && !(Number(kwh) > 0)) return setError("Les kWh doivent être un nombre positif.");
    setIsSaving(true);
    try {
      await chargingService.record({
        vehicleId: kind === "OWN" ? vehicleId : undefined, publicLabel: kind === "PUBLIC" ? publicLabel.trim() : undefined, stationId, amount: Number(amount), method,
        reference: reference.trim() || undefined, batteryBefore: b, batteryAfter: a, kwh: kwh ? Number(kwh) : undefined, requestId: requestId.current,
      });
      addToast({ title: "Recharge enregistrée", description: `Batterie ${b} % → ${a} %.`, type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible d'enregistrer la recharge."));
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Enregistrer une recharge" description="Le client paie la recharge sur place, avec son propre argent." className="max-w-xl">
      <div className="space-y-4">
        <SelectField label="Véhicule *" options={[{ value: "OWN", label: "Véhicule NDZELA" }, { value: "PUBLIC", label: "Voiture publique" }]} value={kind}
          onChange={(event) => { setKind(event.target.value as "OWN" | "PUBLIC"); setError(undefined); }} />
        {kind === "OWN" ? (
          <div>
            <SelectField label="Véhicule NDZELA *" placeholder={vehicles.data?.length === 0 ? "Aucun véhicule avec chauffeur" : "Choisir un véhicule"}
              options={(vehicles.data ?? []).map((option) => ({ value: option.id, label: option.label }))} value={vehicleId} onChange={(event) => touch(setVehicleId)(event.target.value)} />
            {chosen && <p className="mt-1 text-sm text-slate-600">Chauffeur : <strong>{chosen.driverName}</strong></p>}
          </div>
        ) : (
          <FormField label="Plaque ou description *" value={publicLabel} maxLength={100} placeholder="GA-123-AB, Toyota blanche" onChange={(event) => touch(setPublicLabel)(event.target.value)} />
        )}
        <SelectField label="Station *" placeholder={stations.data?.length === 0 ? "Ajoutez d'abord une station" : "Choisir"} options={(stations.data ?? []).map((station) => ({ value: station.id, label: station.name }))}
          value={stationId} onChange={(event) => touch(setStationId)(event.target.value)} />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FormField label="Batterie avant (%) *" inputMode="numeric" value={before} maxLength={3} onChange={(event) => touch(setBefore)(event.target.value)} />
          <FormField label="Solde après (%) *" inputMode="numeric" value={after} maxLength={3} onChange={(event) => touch(setAfter)(event.target.value)} />
          <FormField label="Montant payé (FCFA) *" inputMode="numeric" value={amount} onChange={(event) => touch(setAmount)(event.target.value)} />
          <FormField label="Énergie délivrée (kWh)" inputMode="decimal" value={kwh} hint="Facultatif." onChange={(event) => touch(setKwh)(event.target.value)} />
          <SelectField label="Moyen de paiement *" options={Object.entries(METHOD_LABELS).map(([value, label]) => ({ value, label }))} value={method}
            onChange={(event) => touch(setMethod)(event.target.value as PaymentMethod)} />
          <FormField label={method === "CASH" ? "Référence" : "Référence de la transaction *"} value={reference} maxLength={100} onChange={(event) => touch(setReference)(event.target.value)} />
        </div>
        {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
          <Button onClick={save} isLoading={isSaving}>Enregistrer</Button>
        </div>
      </div>
    </Dialog>
  );
}
