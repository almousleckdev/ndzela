import { describe, expect, it, vi } from "vitest";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { availableActions } from "@/features/drivers/components/driver-actions";
import type { Driver } from "@/features/drivers/types";

// The actions only list what is offered: the services behind them are not needed (and would need the API address).
vi.mock("@/features/drivers/services/drivers.service", () => ({ driversService: {} }));
vi.mock("@/lib/api/api-client", () => ({ apiClient: vi.fn() }));
vi.mock("@/features/drivers/components/close-contract-dialog", () => ({ CloseContractDialog: () => null }));

const driver = { status: "ACTIVE", loginLocked: false, vehicle: null, arrears: { unpaidDays: 0 } } as unknown as Driver;
const withCar = { ...driver, vehicle: {}, contract: {} } as unknown as Driver;
const withoutCar = { ...driver, contract: {} } as unknown as Driver;
const late = (days: number) => ({ ...driver, arrears: { unpaidDays: days } }) as unknown as Driver;
const holding = (...held: Permission[]) => (permission: Permission) => held.includes(permission);

describe("availableActions: helping a driver get back in", () => {
  it("offers the new email/phone and the temporary password with drivers:credentials", () => {
    const kinds = availableActions(driver, holding(PERMISSIONS.DRIVERS_CREDENTIALS)).map((action) => action.kind);

    expect(kinds).toContain("change-contact");
    expect(kinds).toContain("reset-password");
  });

  it("offers neither without the permission", () => {
    const kinds = availableActions(driver, holding(PERMISSIONS.DRIVERS_UPDATE, PERMISSIONS.DRIVERS_SUSPEND)).map((action) => action.kind);

    expect(kinds).not.toContain("change-contact");
    expect(kinds).not.toContain("reset-password");
  });

  it("labels them in French", () => {
    const labels = availableActions(driver, holding(PERMISSIONS.DRIVERS_CREDENTIALS)).map((action) => action.label);

    expect(labels).toEqual(["Modifier l'email ou le téléphone", "Nouveau mot de passe"]);
  });
});

describe("availableActions: closing a contract", () => {
  it("offers 'Clôturer le contrat' only with drivers:update and contracts:terminate", () => {
    const both = availableActions(withCar, holding(PERMISSIONS.DRIVERS_UPDATE, PERMISSIONS.CONTRACTS_TERMINATE)).map((action) => action.label);
    const updateOnly = availableActions(withCar, holding(PERMISSIONS.DRIVERS_UPDATE)).map((action) => action.label);

    expect(both).toContain("Clôturer le contrat");
    expect(updateOnly).not.toContain("Clôturer le contrat");
  });
});

describe("availableActions: a driver whose car was taken keeps his contract", () => {
  const manager = holding(PERMISSIONS.DRIVERS_UPDATE, PERMISSIONS.CONTRACTS_TERMINATE, PERMISSIONS.PAYMENTS_CANCEL_DAYS, PERMISSIONS.PAYMENTS_RESCHEDULE_DAYS);

  it("offers to take the car only while he has one, and the contract actions as long as the contract is open", () => {
    expect(availableActions(withCar, manager).map((action) => action.kind)).toContain("release-vehicle");
    const kinds = availableActions(withoutCar, manager).map((action) => action.kind);

    expect(kinds).not.toContain("release-vehicle");
    expect(kinds).toEqual(expect.arrayContaining(["remove-vehicle", "cancel-days", "reschedule-days"]));
  });

  it("offers rescheduling days only with payments:reschedule_days", () => {
    expect(availableActions(withCar, holding(PERMISSIONS.PAYMENTS_CANCEL_DAYS)).map((action) => action.kind)).not.toContain("reschedule-days");
  });
});

describe("availableActions: partial payment for a driver with 3 late days or more", () => {
  const permission = holding(PERMISSIONS.DRIVERS_PARTIAL_PAYMENT);

  it("is offered from 3 late days with drivers:partial_payment", () => {
    expect(availableActions(late(3), permission).map((action) => action.kind)).toContain("partial-payment");
  });

  it("is not offered below 3 late days, nor without the permission", () => {
    expect(availableActions(late(2), permission).map((action) => action.kind)).not.toContain("partial-payment");
    expect(availableActions(late(5), holding(PERMISSIONS.DRIVERS_UPDATE)).map((action) => action.kind)).not.toContain("partial-payment");
  });
});

describe("availableActions: archiving a driver who resigned", () => {
  it("is offered with drivers:archive, except for a blocked or already archived driver", () => {
    const archive = (status: string, ...held: Permission[]) => availableActions({ ...driver, status } as unknown as Driver, holding(...held)).map((action) => action.kind).includes("archive");

    expect(archive("ACTIVE", PERMISSIONS.DRIVERS_ARCHIVE)).toBe(true);
    expect(archive("SUSPENDED", PERMISSIONS.DRIVERS_ARCHIVE)).toBe(true);
    expect(archive("BLOCKED", PERMISSIONS.DRIVERS_ARCHIVE)).toBe(false);
    expect(archive("ARCHIVED", PERMISSIONS.DRIVERS_ARCHIVE)).toBe(false);
    expect(archive("ACTIVE", PERMISSIONS.DRIVERS_UPDATE)).toBe(false);
  });

  it("lets drivers:update bring an archived driver back", () => {
    const labels = availableActions({ ...driver, status: "ARCHIVED" } as unknown as Driver, holding(PERMISSIONS.DRIVERS_UPDATE)).map((action) => action.label);

    expect(labels).toContain("Réactiver");
  });
});
