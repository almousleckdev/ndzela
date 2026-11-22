import type { DriverOption } from "@/features/free-charging/types";

export const DRIVER_PAGE_SIZE = 25;
export type Membership = "ALL" | "IN_PROGRAM" | "NOT_IN_PROGRAM";

export interface Selection {
  adds: Record<string, DriverOption>;
  removes: Record<string, DriverOption>;
}

export const NO_SELECTION: Selection = { adds: {}, removes: {} };

export const canAdd = (driver: DriverOption) => !driver.inProgram && driver.linked && driver.status === "ACTIVE";
export const canRemove = (driver: DriverOption) => driver.inProgram && Boolean(driver.agreementId);
export const isToggleable = (driver: DriverOption) => canAdd(driver) || canRemove(driver);

// A driver in the program is ticked until staff untick them; any other driver is ticked once staff tick them.
export const isChecked = (driver: DriverOption, selection: Selection) =>
  driver.inProgram ? !(driver.driverId in selection.removes) : driver.driverId in selection.adds;

const without = <T>(record: Record<string, T>, key: string) => Object.fromEntries(Object.entries(record).filter(([id]) => id !== key));

export function toggle(driver: DriverOption, selection: Selection): Selection {
  const key = driver.driverId;
  const side = driver.inProgram ? "removes" : "adds";
  const current = selection[side];
  return { ...selection, [side]: key in current ? without(current, key) : { ...current, [key]: driver } };
}

export function toggleAll(drivers: DriverOption[], selection: Selection, check: boolean): Selection {
  return drivers.filter(isToggleable).reduce((current, driver) => (isChecked(driver, current) === check ? current : toggle(driver, current)), selection);
}

export const pendingCounts = (selection: Selection) => ({ add: Object.keys(selection.adds).length, remove: Object.keys(selection.removes).length });
