import { describe, expect, it, vi } from "vitest";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";
import { availableActions } from "@/features/drivers/components/driver-actions";
import type { Driver } from "@/features/drivers/types";

// The actions only list what is offered: the services behind them are not needed (and would need the API address).
vi.mock("@/features/drivers/services/drivers.service", () => ({ driversService: {} }));
vi.mock("@/lib/api/api-client", () => ({ apiClient: vi.fn() }));
vi.mock("@/features/drivers/components/close-contract-dialog", () => ({ CloseContractDialog: () => null }));

const driver = { status: "ACTIVE", loginLocked: false, vehicle: null, arrears: { unpaidDays: 0 } } as unknown as Driver;
const withCar = { ...driver, vehicle: {} } as unknown as Driver;
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
