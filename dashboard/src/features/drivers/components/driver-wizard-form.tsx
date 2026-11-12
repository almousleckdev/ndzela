"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import type { PhoneCodeOption } from "@/components/ui/phone-field";
import { Card } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import type { Country } from "@/features/auth/types";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { EmergencyStep } from "@/features/drivers/components/steps/emergency-step";
import { PersonalStep } from "@/features/drivers/components/steps/personal-step";
import { ContractStep } from "@/features/drivers/components/steps/contract-step";
import { TrainingStep } from "@/features/drivers/components/steps/training-step";
import { PasswordDialog } from "@/features/drivers/components/password-dialog";
import { EMPTY_DRIVER, NO_UPLOADS, toRequestBody, valuesFromDriver, type PhoneCodes, type UploadKey, type Uploads } from "@/features/drivers/lib/driver-form";
import { driverSchema, type DriverFormValues } from "@/features/drivers/schemas/driver";
import { driversService } from "@/features/drivers/services/drivers.service";
import type { Driver, TrainingCenter } from "@/features/drivers/types";
import { phoneCodeOptions } from "@/features/tenancy/lib/phone-codes";
import { useCountrySelectionStore } from "@/features/tenancy/store/country-selection.store";
import type { Vehicle } from "@/features/vehicles/types";

const STEPS = [
  { key: "personal", title: "Informations personnelles" },
  { key: "emergency", title: "Personne à contacter en cas d'urgence" },
  { key: "training", title: "Véhicule et formation" },
  { key: "contract", title: "Contrat et récapitulatif financier" },
] as const;

interface DriverWizardFormProps {
  driver?: Driver;
  countries: Country[] | null;
  centers: TrainingCenter[];
  vehicles: Vehicle[] | null;
}

export function DriverWizardForm({ driver, countries, centers, vehicles }: DriverWizardFormProps) {
  const router = useRouter();
  const { addToast } = useToast();
  const { can } = usePermissions();
  const me = useAuthStore((state) => state.user);
  const selectedCountry = useCountrySelectionStore((state) => state.selectedCode);
  const [step, setStep] = useState(0);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [codeChoice, setCodeChoice] = useState<Partial<PhoneCodes>>({});
  const [uploads, setUploads] = useState<Uploads>(NO_UPLOADS);
  const [uploadErrors, setUploadErrors] = useState<Partial<Record<UploadKey, string>>>({});

  const pick = (key: UploadKey, file: File | null, error?: string) => {
    setUploads((current) => ({ ...current, [key]: file }));
    setUploadErrors((current) => ({ ...current, [key]: error }));
  };

  // A driver belongs to one country, so only its dialling code is offered
  const ownCountry = driver?.country?.code ?? me?.country?.code ?? selectedCountry;
  const phoneCodes = phoneCodeOptions(ownCountry && countries ? countries.filter((country) => country.code === ownCountry) : countries, me?.country ?? null);
  const knownCodes = [driver?.country?.phoneCode, me?.country?.phoneCode, ...phoneCodes.map((option) => option.value)].filter((code): code is string => Boolean(code));
  const initial = driver ? valuesFromDriver(driver, knownCodes) : null;
  const fallbackCode = driver?.country?.phoneCode
    ?? countries?.find((country) => country.code === selectedCountry)?.phoneCode
    ?? me?.country?.phoneCode
    ?? phoneCodes[0]?.value
    ?? "";
  const codes: PhoneCodes = {
    primary: codeChoice.primary ?? initial?.codes.primary ?? fallbackCode,
    secondary: codeChoice.secondary ?? initial?.codes.secondary ?? fallbackCode,
    emergency: codeChoice.emergency ?? initial?.codes.emergency ?? fallbackCode,
  };

  const form = useForm<DriverFormValues>({ resolver: zodResolver(driverSchema), defaultValues: initial?.values ?? EMPTY_DRIVER });
  const mayUpdateContract = Boolean(driver?.contract) && can(PERMISSIONS.CONTRACTS_UPDATE);
  const [updateContract, setUpdateContract] = useState(true);
  const [startDate, setStartDate] = useState(driver?.contract?.startDate ?? "");
  const steps = driver && !mayUpdateContract ? STEPS.slice(0, 3) : STEPS;
  const isLastStep = step === steps.length - 1;

  const missingPhoto = () => {
    if (driver || uploads.photo) return false;
    setUploadErrors((current) => ({ ...current, photo: "La photo de profil est obligatoire." }));
    return true;
  };

  const goNext = async () => {
    const valid = await form.trigger(steps[step].key);
    const photoMissing = step === 0 && missingPhoto();
    if (valid && !photoMissing && !uploadErrors.photo) setStep(step + 1);
  };

  const finish = async () => {
    const valid = await form.trigger();
    if (!valid) {
      const errors = form.formState.errors;
      setStep(errors.personal ? 0 : errors.emergency ? 1 : errors.vehicleId || errors.training ? 2 : 3);
      return;
    }
    if (missingPhoto()) {
      setStep(0);
      return;
    }
    if (Object.values(uploadErrors).some(Boolean)) return;
    if (!driver && !form.getValues("vehicleId")) {
      form.setError("vehicleId", { message: "Choisissez un véhicule" });
      return;
    }
    if (driver) await save(driver);
    else setPasswordOpen(true);
  };

  const save = async (existing: Driver) => {
    try {
      const newVehicleId = mayUpdateContract ? form.getValues("vehicleId") : "";
      // The start date goes first: it is the change most likely to be refused.
      const startChanged = Boolean(mayUpdateContract && existing.contract && startDate && startDate !== existing.contract.startDate);
      if (startChanged) await driversService.changeContractStart(existing.id, startDate);
      await driversService.update(existing.id, toRequestBody(form.getValues(), codes));
      const vehicleChanged = Boolean(newVehicleId && newVehicleId !== existing.vehicle?.id);
      if (vehicleChanged) await driversService.changeVehicle(existing.id, newVehicleId);
      const deposit = Number(form.getValues("contract.deposit"));
      // A car change already refreshed the printed parties, so the contract is refreshed again only for a new deposit.
      if (mayUpdateContract && updateContract && (!vehicleChanged || deposit !== existing.contract?.deposit)) {
        const contract = await driversService.updateContract(existing.id, deposit);
        addToast({ title: "Succès", description: "Chauffeur et contrat mis à jour.", type: "success" });
        router.push(`/contracts/${contract.id}`);
        return;
      }
      if (existing.contract && (vehicleChanged || startChanged)) {
        addToast({ title: "Succès", description: "Chauffeur et contrat mis à jour.", type: "success" });
        router.push(`/contracts/${existing.contract.id}`);
        return;
      }
      addToast({ title: "Succès", description: "Chauffeur mis à jour.", type: "success" });
      router.push(`/drivers/${existing.id}`);
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible d'enregistrer le chauffeur."), type: "error" });
    }
  };

  const create = async (password: string) => {
    try {
      const values = form.getValues();
      const created = await driversService.create(toRequestBody(values, codes, { vehicleId: values.vehicleId, password }), uploads);
      addToast({ title: "Succès", description: "Chauffeur créé.", type: "success" });
      router.push(created.contract ? `/contracts/${created.contract.id}` : `/drivers/${created.id}`);
    } catch (error) {
      setPasswordOpen(false);
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de créer le chauffeur."), type: "error" });
    }
  };

  const phoneOptions = withCurrentCodes(phoneCodes, codes);

  return (
    <>
      <Card>
        <div className="mb-6">
          <p className="text-xs font-medium uppercase tracking-wider text-slate-500">Étape {step + 1} sur {steps.length}</p>
          <h2 className="mt-1 font-display text-xl font-semibold text-slate-900">{steps[step].title}</h2>
          <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${((step + 1) / steps.length) * 100}%` }} />
          </div>
        </div>

        <div hidden={step !== 0}><PersonalStep form={form} phoneCodes={phoneOptions} codes={codes} onCodeChange={(key, code) => setCodeChoice((c) => ({ ...c, [key]: code }))}
          photo={driver ? undefined : { file: uploads.photo, error: uploadErrors.photo, onChange: (file, error) => pick("photo", file, error) }} /></div>
        <div hidden={step !== 1}><EmergencyStep form={form} phoneCodes={phoneOptions} codes={codes} onCodeChange={(key, code) => setCodeChoice((c) => ({ ...c, [key]: code }))} /></div>
        {(!driver || mayUpdateContract) && <div hidden={step !== 3}><ContractStep form={form} existing={driver?.contract ?? undefined} updateExisting={updateContract} onToggleUpdate={setUpdateContract} startDate={startDate} onStartDateChange={setStartDate} /></div>}
        <div hidden={step !== 2}><TrainingStep form={form} centers={centers} vehicles={driver && !(mayUpdateContract && driver.vehicle) ? null : vehicles} currentVehicle={driver?.vehicle} uploads={driver ? undefined : { files: uploads, errors: uploadErrors, onPick: pick }} /></div>

        <div className="mt-8 flex justify-between border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={() => (step === 0 ? router.back() : setStep(step - 1))}>{step === 0 ? "Annuler" : "Précédent"}</Button>
          {isLastStep
            ? <Button onClick={finish} isLoading={form.formState.isSubmitting}>{driver ? "Enregistrer" : "Continuer"}</Button>
            : <Button onClick={goNext}>Suivant</Button>}
        </div>
      </Card>
      {passwordOpen && <PasswordDialog email={form.getValues("personal.email")} onClose={() => setPasswordOpen(false)} onConfirm={create} />}
    </>
  );
}

// The dropdown also offers the codes already in use
function withCurrentCodes(options: PhoneCodeOption[], codes: PhoneCodes): PhoneCodeOption[] {
  const extra = Object.values(codes).filter((code, index, all) => code && all.indexOf(code) === index && !options.some((option) => option.value === code));
  return [...options, ...extra.map((code) => ({ value: code, label: code }))];
}
