"use client";

import { useState } from "react";
import { getErrorMessage } from "@/lib/errors";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { AgreementInput, DriverOption } from "@/features/free-charging/types";

export interface CreateFailure {
  driver: DriverOption;
  message: string;
}

// Adds the chosen drivers one after the other: one agreement each, so one refusal never blocks the others.
export function useCreateMany() {
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [added, setAdded] = useState(0);
  const [failures, setFailures] = useState<CreateFailure[]>([]);

  const run = async (drivers: DriverOption[], terms: AgreementInput): Promise<CreateFailure[]> => {
    const failed: CreateFailure[] = [];
    let created = 0;
    setProgress({ done: 0, total: drivers.length });
    for (const [index, driver] of drivers.entries()) {
      try {
        await freeChargingService.create(driver.driverId, terms);
        created += 1;
      } catch (error) {
        failed.push({ driver, message: getErrorMessage(error, "L'accord n'a pas pu être créé.") });
      }
      setProgress({ done: index + 1, total: drivers.length });
    }
    setAdded((before) => before + created);
    setFailures(failed);
    setProgress(null);
    return failed;
  };

  return { run, progress, added, failures, running: progress !== null };
}
