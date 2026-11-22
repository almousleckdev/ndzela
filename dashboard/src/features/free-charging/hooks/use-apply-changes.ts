"use client";

import { useState } from "react";
import { getErrorMessage } from "@/lib/errors";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { AgreementInput, DriverOption } from "@/features/free-charging/types";

export interface ChangeFailure {
  driver: DriverOption;
  message: string;
}

export interface ChangeResult {
  added: number;
  removed: number;
  failures: ChangeFailure[];
}

// Applies the ticks one after the other (one agreement each) so one refusal never blocks the others.
export function useApplyChanges() {
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [result, setResult] = useState<ChangeResult | null>(null);

  const run = async (adds: DriverOption[], removes: DriverOption[], terms: AgreementInput): Promise<ChangeFailure[]> => {
    const total = adds.length + removes.length;
    const failures: ChangeFailure[] = [];
    let done = 0;
    let added = 0;
    let removed = 0;
    setProgress({ done, total });
    const attempt = async (driver: DriverOption, action: () => Promise<unknown>, onDone: () => void) => {
      try {
        await action();
        onDone();
      } catch (error) {
        failures.push({ driver, message: getErrorMessage(error, "Le changement n'a pas pu être enregistré.") });
      }
      done += 1;
      setProgress({ done, total });
    };
    for (const driver of adds) await attempt(driver, () => freeChargingService.create(driver.driverId, terms), () => (added += 1));
    for (const driver of removes) await attempt(driver, () => freeChargingService.revoke(driver.agreementId ?? ""), () => (removed += 1));
    setResult((before) => ({ added: (before?.added ?? 0) + added, removed: (before?.removed ?? 0) + removed, failures }));
    setProgress(null);
    return failures;
  };

  return { run, progress, result, running: progress !== null };
}
