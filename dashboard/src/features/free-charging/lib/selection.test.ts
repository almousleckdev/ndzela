import { describe, expect, it } from "vitest";
import type { DriverOption } from "@/features/free-charging/types";
import { NO_SELECTION, canAdd, isChecked, isToggleable, pendingCounts, toggle, toggleAll } from "./selection";

const driver = (overrides: Partial<DriverOption> = {}): DriverOption => ({
  driverId: "d1", customerId: "c1", fullName: "MBA Jean", phone: null, email: null, fleetNumber: null, plate: null, status: "ACTIVE",
  linked: true, inProgram: false, agreementId: null, agreementStatus: null, ...overrides,
});
const member = (id: string) => driver({ driverId: id, inProgram: true, agreementId: `ag-${id}`, agreementStatus: "ACTIVE" });

describe("selection", () => {
  it("lets staff add only a linked, active driver who is not in the program", () => {
    expect(canAdd(driver())).toBe(true);
    expect(canAdd(driver({ linked: false }))).toBe(false);
    expect(canAdd(driver({ status: "ARCHIVED" }))).toBe(false);
    expect(canAdd(member("m"))).toBe(false);
  });

  it("lets staff remove a driver who has an agreement, whatever their status", () => {
    expect(isToggleable(member("m"))).toBe(true);
    expect(isToggleable(driver({ inProgram: true, agreementId: null }))).toBe(false);
    expect(isToggleable({ ...member("m"), status: "ARCHIVED" })).toBe(true);
  });

  it("shows members ticked and others unticked, and flips them on toggle", () => {
    const jean = driver();
    const awa = member("awa");
    expect(isChecked(jean, NO_SELECTION)).toBe(false);
    expect(isChecked(awa, NO_SELECTION)).toBe(true);

    const toggled = toggle(awa, toggle(jean, NO_SELECTION));
    expect(isChecked(jean, toggled)).toBe(true);
    expect(isChecked(awa, toggled)).toBe(false);
    expect(pendingCounts(toggled)).toEqual({ add: 1, remove: 1 });
    expect(pendingCounts(toggle(awa, toggle(jean, toggled)))).toEqual({ add: 0, remove: 0 });
  });

  it("ticks or unticks a whole page without touching blocked drivers", () => {
    const page = [driver({ driverId: "a" }), driver({ driverId: "b" }), driver({ driverId: "c", linked: false }), member("m")];

    const all = toggleAll(page, NO_SELECTION, true);
    expect(Object.keys(all.adds)).toEqual(["a", "b"]);
    expect(pendingCounts(all).remove).toBe(0);
    expect(pendingCounts(toggleAll(page, all, false))).toEqual({ add: 0, remove: 1 });
  });
});
